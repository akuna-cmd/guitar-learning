package com.guitarlearning.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import android.util.Base64
import com.guitarlearning.data.local.AudioNoteDao
import com.guitarlearning.data.local.TextNoteDao
import com.guitarlearning.data.settings.AppSettingsRepository
import com.guitarlearning.data.settings.AppSettingsSnapshot
import com.guitarlearning.domain.model.AudioNote
import com.guitarlearning.domain.model.Difficulty
import com.guitarlearning.domain.model.Goal
import com.guitarlearning.domain.model.GoalType
import com.guitarlearning.domain.model.PracticedTab
import com.guitarlearning.domain.model.Session
import com.guitarlearning.domain.model.DEFAULT_TAB_FOLDER_KEY
import com.guitarlearning.domain.model.TabItem
import com.guitarlearning.domain.model.TabPlaybackProgress
import com.guitarlearning.domain.model.TextNote
import com.guitarlearning.domain.model.normalizeTabFolder
import com.guitarlearning.domain.repository.GoalRepository
import com.guitarlearning.domain.repository.SessionRepository
import com.guitarlearning.domain.repository.SyncRepository
import com.guitarlearning.domain.repository.TabPlaybackProgressRepository
import com.guitarlearning.domain.repository.TabRepository
import com.guitarlearning.presentation.main.AiProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.Timestamp
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.io.File
import java.security.MessageDigest
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreSyncRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tabRepository: TabRepository,
    private val sessionRepository: SessionRepository,
    private val goalRepository: GoalRepository,
    private val progressRepository: TabPlaybackProgressRepository,
    private val audioNoteDao: AudioNoteDao,
    private val textNoteDao: TextNoteDao,
    private val appSettingsRepository: AppSettingsRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : SyncRepository {
    private companion object {
        const val LogTag = "CloudSync"
        const val MaxInlineUserTabBytes = 512 * 1024L
        const val MaxInlineAudioNoteBytes = 1024 * 1024L
    }

    private data class CloudTabFilePayload(
        val storagePath: String?,
        val fileBase64: String?
    )

    private data class RemoteTabsImportResult(
        val tabs: List<TabItem>,
        val unresolvedRemoteIds: Set<String>
    )

    private data class RemoteImportResult<T>(
        val items: List<T>,
        val unresolvedRemoteIds: Set<String>
    )

    private data class CloudAudioNoteFilePayload(
        val storagePath: String?,
        val fileBase64: String?
    )

    private val syncingState = MutableStateFlow(false)

    override fun isSyncing(): Flow<Boolean> = syncingState

    override fun getLastSyncedTime(): Flow<Long?> = appSettingsRepository.observeLastCloudSyncAt()

    override suspend fun deleteUserTab(tab: TabItem): Result<Unit> {
        val user = auth.currentUser ?: return Result.success(Unit)
        return runCatching {
            firestore.collection("users")
                .document(user.uid)
                .collection("tabs")
                .document(tab.id)
                .delete()
                .await()
            deleteUserTabFile(user.uid, tab.id)
            Unit
        }
    }

    override suspend fun syncData(): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception(context.getString(com.guitarlearning.R.string.sync_error_not_authorized)))

        syncingState.value = true
        return runCatching {
            Log.d(LogTag, "syncData:start uid=${user.uid} email=${user.email.orEmpty()}")
            val userRef = firestore.collection("users").document(user.uid)
            val previousOwnerUid = appSettingsRepository.getSyncOwnerUid()
            val pendingDeletedUserTabIds = appSettingsRepository.getPendingDeletedUserTabIds()
            val isAccountSwitch = !previousOwnerUid.isNullOrBlank() && previousOwnerUid != user.uid

            Log.d(
                LogTag,
                "syncData:context previousOwnerUid=${previousOwnerUid.orEmpty()} pendingDeletedTabs=${pendingDeletedUserTabIds.size} isAccountSwitch=$isAccountSwitch"
            )

            if (isAccountSwitch) {
                Log.w(LogTag, "syncData:accountSwitch clearing local cloud-scoped data for uid=${user.uid}")
                clearLocalCloudScopedData()
                ensureBuiltInTabsSeeded()
            }

            val remoteSettingsDocument = userRef.collection("settings").document("app").get().await()
            val remoteSettings = remoteSettingsDocument.toSettings()
            val remoteTabsSnapshot = userRef.collection("tabs").get().await()
            val remoteSessionsSnapshot = userRef.collection("sessions").get().await()
            val remoteGoalsSnapshot = userRef.collection("goals").get().await()
            val remoteProgressSnapshot = userRef.collection("progress").get().await()
            val remoteTextNotesSnapshot = userRef.collection("text_notes").get().await()
            val remoteAudioNotesSnapshot = userRef.collection("audio_notes").get().await()
            val remoteStoragePathsByTabId = remoteTabsSnapshot.documents.associate { it.id to it.getString("storagePath") }
            val remoteAudioStoragePathsById = remoteAudioNotesSnapshot.documents.associate { it.id to it.getString("storagePath") }
            val filteredRemoteTabDocuments = remoteTabsSnapshot.documents.filterNot { document ->
                val remoteId = document.getString("id") ?: document.id
                document.getBoolean("isUserTab") == true && remoteId in pendingDeletedUserTabIds
            }
            val remoteTabsImport = importRemoteTabs(filteredRemoteTabDocuments)
            val remoteTabs = remoteTabsImport.tabs
            val settingsSessionBackups = remoteSettingsDocument.toSessionBackups()
            val settingsTextNoteBackups = remoteSettingsDocument.toTextNoteBackups()
            val settingsAudioNoteBackups = remoteSettingsDocument.toAudioNoteBackups()
            val usedSessionFallback = remoteSessionsSnapshot.isEmpty
            val remoteSessionsImport = if (usedSessionFallback) {
                RemoteImportResult(
                    items = settingsSessionBackups,
                    unresolvedRemoteIds = emptySet()
                )
            } else {
                importRemoteSessions(remoteSessionsSnapshot.documents)
            }
            val remoteSessions = if (remoteSessionsImport.items.isEmpty() &&
                remoteSessionsImport.unresolvedRemoteIds.isNotEmpty() &&
                settingsSessionBackups.isNotEmpty()
            ) {
                Log.w(LogTag, "syncData:using settings session backups because collection docs are unreadable")
                settingsSessionBackups
            } else {
                remoteSessionsImport.items
            }
            val remoteGoals = remoteGoalsSnapshot.documents.mapNotNull { it.toGoal() }
            val remoteProgress = remoteProgressSnapshot.documents.mapNotNull { it.toProgress() }
            val usedTextNoteFallback = remoteTextNotesSnapshot.isEmpty
            val remoteTextNotesImport = if (usedTextNoteFallback) {
                RemoteImportResult(
                    items = settingsTextNoteBackups,
                    unresolvedRemoteIds = emptySet()
                )
            } else {
                importRemoteTextNotes(remoteTextNotesSnapshot.documents)
            }
            val remoteTextNotes = if (remoteTextNotesImport.items.isEmpty() &&
                remoteTextNotesImport.unresolvedRemoteIds.isNotEmpty() &&
                settingsTextNoteBackups.isNotEmpty()
            ) {
                Log.w(LogTag, "syncData:using settings text note backups because collection docs are unreadable")
                settingsTextNoteBackups
            } else {
                remoteTextNotesImport.items
            }
            val usedAudioNoteFallback = remoteAudioNotesSnapshot.isEmpty
            val remoteAudioNotesImport = if (usedAudioNoteFallback) {
                RemoteImportResult(
                    items = settingsAudioNoteBackups,
                    unresolvedRemoteIds = emptySet()
                )
            } else {
                importRemoteAudioNotes(remoteAudioNotesSnapshot.documents)
            }
            val remoteAudioNotes = if (remoteAudioNotesImport.items.isEmpty() &&
                remoteAudioNotesImport.unresolvedRemoteIds.isNotEmpty() &&
                settingsAudioNoteBackups.isNotEmpty()
            ) {
                Log.w(LogTag, "syncData:using settings audio note backups because collection docs are unreadable")
                settingsAudioNoteBackups
            } else {
                remoteAudioNotesImport.items
            }

            Log.d(
                LogTag,
                "syncData:remote uid=${user.uid} tabs=${remoteTabs.size}/${remoteTabsSnapshot.size()} unresolvedTabs=${remoteTabsImport.unresolvedRemoteIds.size} " +
                    "sessions=${remoteSessions.size}/${remoteSessionsSnapshot.size()} unresolvedSessions=${remoteSessionsImport.unresolvedRemoteIds.size} sessionFallback=$usedSessionFallback " +
                    "goals=${remoteGoals.size}/${remoteGoalsSnapshot.size()} progress=${remoteProgress.size}/${remoteProgressSnapshot.size()} " +
                    "textNotes=${remoteTextNotes.size}/${remoteTextNotesSnapshot.size()} unresolvedTextNotes=${remoteTextNotesImport.unresolvedRemoteIds.size} textFallback=$usedTextNoteFallback " +
                    "audioNotes=${remoteAudioNotes.size}/${remoteAudioNotesSnapshot.size()} unresolvedAudioNotes=${remoteAudioNotesImport.unresolvedRemoteIds.size} audioFallback=$usedAudioNoteFallback"
            )

            val localSettings = appSettingsRepository.getSettings()
            val mergedSettings = mergeSettings(localSettings, remoteSettings)
            appSettingsRepository.replaceSettings(mergedSettings)

            val localTabs = tabRepository.getAllTabsSync()
            val mergedTabsResult = mergeTabCollections(localTabs, remoteTabs)
            if (mergedTabsResult.tabsToDelete.isNotEmpty()) {
                tabRepository.deleteTabs(mergedTabsResult.tabsToDelete)
            }
            if (mergedTabsResult.tabs.isNotEmpty()) {
                tabRepository.upsertTabs(mergedTabsResult.tabs)
            }

            sessionRepository.importHistory(remoteSessions)

            val localGoals = goalRepository.getGoalsSync()
            val mergedGoals = mergeGoals(localGoals, remoteGoals)
            goalRepository.clearGoals()
            if (mergedGoals.isNotEmpty()) {
                goalRepository.upsertGoals(mergedGoals)
            }

            val localProgress = progressRepository.observeAll().first()
            val mergedProgress = mergeProgress(localProgress, remoteProgress)
            progressRepository.replaceAll(mergedProgress)

            val localTextNotes = textNoteDao.getAllTextNotes()
            val useRemoteNotesAsSourceOfTruth = previousOwnerUid.isNullOrBlank() || isAccountSwitch
            val mergedTextNotes = if (useRemoteNotesAsSourceOfTruth) {
                mergeTextNotes(localTextNotes, remoteTextNotes)
            } else {
                localTextNotes.sortedByDescending { it.createdAt.time }
            }
            textNoteDao.clearAll()
            if (mergedTextNotes.isNotEmpty()) {
                textNoteDao.insertAllTextNotes(mergedTextNotes.map { it.copy(id = 0) })
            }

            val localAudioNotes = audioNoteDao.getAllNotes()
            val mergedAudioNotes = if (useRemoteNotesAsSourceOfTruth) {
                mergeAudioNotes(localAudioNotes, remoteAudioNotes)
            } else {
                localAudioNotes.sortedByDescending { it.createdAt.time }
            }
            audioNoteDao.clearAll()
            if (mergedAudioNotes.isNotEmpty()) {
                audioNoteDao.insertAll(mergedAudioNotes.map { it.copy(id = 0) })
            }

            Log.d(
                LogTag,
                "syncData:merge uid=${user.uid} localTabs=${localTabs.size} localSessionsBefore=${sessionRepository.getAllSessionsSync().size} " +
                    "localGoals=${localGoals.size} localProgress=${localProgress.size} localTextNotes=${localTextNotes.size} localAudioNotes=${localAudioNotes.size} " +
                    "mergedTextNotes=${mergedTextNotes.size} mergedAudioNotes=${mergedAudioNotes.size} useRemoteNotesAsSourceOfTruth=$useRemoteNotesAsSourceOfTruth"
            )

            val finalLocalTabs = tabRepository.getAllTabsSync()
            val finalLocalSessions = sessionRepository.getAllSessionsSync()
            val finalLocalGoals = goalRepository.getGoalsSync()
            val finalLocalProgress = progressRepository.observeAll().first()
            val finalLocalTextNotes = textNoteDao.getAllTextNotes()
            val finalLocalAudioNotes = audioNoteDao.getAllNotes()
            val finalLocalSettings = appSettingsRepository.getSettings()
            val remoteTabIds = remoteTabsSnapshot.documents.map { it.id }.toSet()
            val remoteUserTabIds = remoteTabsSnapshot.documents
                .filter { it.getBoolean("isUserTab") == true }
                .map { it.getString("id") ?: it.id }
                .toSet()
            val remoteAudioNoteIds = remoteAudioNotesSnapshot.documents.map { it.id }.toSet()
            val finalLocalTabIds = finalLocalTabs.map { it.id }.toSet() + remoteTabsImport.unresolvedRemoteIds
            val finalLocalUserTabIds = finalLocalTabs
                .filter { it.isUserTab }
                .map { it.id }
                .toSet() + remoteTabsImport.unresolvedRemoteIds
            val finalLocalSessionIds = finalLocalSessions.map(::sessionDocumentId).toSet() + remoteSessionsImport.unresolvedRemoteIds
            val finalLocalTextNoteIds = finalLocalTextNotes.map(::textNoteDocumentId).toSet() + remoteTextNotesImport.unresolvedRemoteIds
            val finalLocalAudioNoteIds = finalLocalAudioNotes.map(::audioNoteDocumentId).toSet() + remoteAudioNotesImport.unresolvedRemoteIds
            val uploadedStoragePaths = finalLocalTabs
                .filter { it.isUserTab }
                .associate { tab ->
                    tab.id to prepareCloudTabFilePayload(
                        userId = user.uid,
                        tab = tab,
                        existingStoragePath = remoteStoragePathsByTabId[tab.id]
                    )
                }
            val uploadedAudioStoragePaths = finalLocalAudioNotes.associate { audioNote ->
                val documentId = audioNoteDocumentId(audioNote)
                documentId to prepareCloudAudioNoteFilePayload(
                    userId = user.uid,
                    documentId = documentId,
                    audioNote = audioNote,
                    existingStoragePath = remoteAudioStoragePathsById[documentId]
                )
            }

            val batch = firestore.batch()

            val settingsRef = userRef.collection("settings").document("app")
            batch.set(
                settingsRef,
                finalLocalSettings.toFirestoreMap().toMutableMap<String, Any?>().apply {
                    put(
                        "sessionBackups",
                        if (remoteSessionsImport.unresolvedRemoteIds.isNotEmpty() && settingsSessionBackups.isNotEmpty()) {
                            settingsSessionBackups.map { it.toFirestoreMap() }
                        } else {
                            finalLocalSessions.map { it.toFirestoreMap() }
                        }
                    )
                    put(
                        "textNoteBackups",
                        if (remoteTextNotesImport.unresolvedRemoteIds.isNotEmpty() && settingsTextNoteBackups.isNotEmpty()) {
                            settingsTextNoteBackups.map { it.toFirestoreMap() }
                        } else {
                            finalLocalTextNotes.map { it.toFirestoreMap() }
                        }
                    )
                    put(
                        "audioNoteBackups",
                        if (remoteAudioNotesImport.unresolvedRemoteIds.isNotEmpty() && settingsAudioNoteBackups.isNotEmpty()) {
                            settingsAudioNoteBackups.map { audioNote ->
                                val documentId = audioNoteDocumentId(audioNote)
                                audioNote.toFirestoreBackupMap(
                                    documentId = documentId,
                                    filePayload = uploadedAudioStoragePaths[documentId]
                                )
                            }
                        } else {
                            finalLocalAudioNotes.map { audioNote ->
                                val documentId = audioNoteDocumentId(audioNote)
                                audioNote.toFirestoreBackupMap(
                                    documentId = documentId,
                                    filePayload = uploadedAudioStoragePaths[documentId]
                                )
                            }
                        }
                    )
                },
                SetOptions.merge()
            )

            finalLocalTabs.forEach { tab ->
                val tabRef = userRef.collection("tabs").document(tab.id)
                batch.set(
                    tabRef,
                    tab.toFirestoreMap(uploadedStoragePaths[tab.id]),
                    SetOptions.merge()
                )
            }
            deleteStaleUserTabFiles(user.uid, remoteUserTabIds, finalLocalUserTabIds)
            deleteStaleDocuments(
                batch = batch,
                parent = userRef.collection("tabs"),
                remoteIds = remoteTabIds,
                localIds = finalLocalTabIds
            )

            finalLocalSessions.forEach { session ->
                val sessionRef = userRef.collection("sessions").document(sessionDocumentId(session))
                batch.set(sessionRef, session.toFirestoreMap(), SetOptions.merge())
            }
            deleteStaleDocuments(
                batch = batch,
                parent = userRef.collection("sessions"),
                remoteIds = remoteSessionsSnapshot.documents.map { it.id }.toSet(),
                localIds = finalLocalSessionIds
            )

            finalLocalGoals.forEach { goal ->
                val goalRef = userRef.collection("goals").document(goal.syncId)
                batch.set(goalRef, goal.toFirestoreMap(), SetOptions.merge())
            }
            deleteStaleDocuments(
                batch = batch,
                parent = userRef.collection("goals"),
                remoteIds = remoteGoalsSnapshot.documents.map { it.id }.toSet(),
                localIds = finalLocalGoals.map { it.syncId }.toSet()
            )

            finalLocalProgress.forEach { progress ->
                val progressRef = userRef.collection("progress").document(progress.tabId)
                batch.set(progressRef, progress.toFirestoreMap(), SetOptions.merge())
            }
            deleteStaleDocuments(
                batch = batch,
                parent = userRef.collection("progress"),
                remoteIds = remoteProgressSnapshot.documents.map { it.id }.toSet(),
                localIds = finalLocalProgress.map { it.tabId }.toSet()
            )

            finalLocalTextNotes.forEach { textNote ->
                val documentId = textNoteDocumentId(textNote)
                val textNoteRef = userRef.collection("text_notes").document(documentId)
                batch.set(textNoteRef, textNote.toFirestoreMap(), SetOptions.merge())
            }
            deleteStaleDocuments(
                batch = batch,
                parent = userRef.collection("text_notes"),
                remoteIds = remoteTextNotesSnapshot.documents.map { it.id }.toSet(),
                localIds = finalLocalTextNoteIds
            )

            finalLocalAudioNotes.forEach { audioNote ->
                val documentId = audioNoteDocumentId(audioNote)
                val audioNoteRef = userRef.collection("audio_notes").document(documentId)
                batch.set(
                    audioNoteRef,
                    audioNote.toFirestoreMap(uploadedAudioStoragePaths[documentId]),
                    SetOptions.merge()
                )
            }
            deleteStaleAudioNoteFiles(user.uid, remoteAudioNoteIds, finalLocalAudioNoteIds, remoteAudioStoragePathsById)
            deleteStaleDocuments(
                batch = batch,
                parent = userRef.collection("audio_notes"),
                remoteIds = remoteAudioNoteIds,
                localIds = finalLocalAudioNoteIds
            )

            Log.d(
                LogTag,
                "syncData:write uid=${user.uid} finalTabs=${finalLocalTabs.size} finalSessions=${finalLocalSessions.size} finalGoals=${finalLocalGoals.size} " +
                    "finalProgress=${finalLocalProgress.size} finalTextNotes=${finalLocalTextNotes.size} finalAudioNotes=${finalLocalAudioNotes.size} " +
                    "backupSessions=${finalLocalSessions.size} backupTextNotes=${finalLocalTextNotes.size} backupAudioNotes=${finalLocalAudioNotes.size}"
            )

            batch.commit().await()

            val syncTimestamp = System.currentTimeMillis()
            appSettingsRepository.clearPendingDeletedUserTabIds(pendingDeletedUserTabIds)
            appSettingsRepository.setSyncOwnerUid(user.uid)
            appSettingsRepository.setLastCloudSyncAt(syncTimestamp)
            Log.d(LogTag, "syncData:success uid=${user.uid} syncedAt=$syncTimestamp")
            Unit
        }
            .onFailure { error ->
                Log.e(LogTag, "syncData:failure uid=${user.uid}", error)
            }
            .also {
                syncingState.value = false
            }
    }

    override suspend fun clearRemoteData(): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception(context.getString(com.guitarlearning.R.string.sync_error_not_authorized)))
        syncingState.value = true
        return runCatching {
            Log.w(LogTag, "clearRemoteData:start uid=${user.uid}")
            val userRef = firestore.collection("users").document(user.uid)
            val collections = listOf("tabs", "sessions", "goals", "progress", "text_notes", "audio_notes")
            collections.forEach { name ->
                val snapshot = userRef.collection(name).get().await()
                if (snapshot.isEmpty) return@forEach
                val batch = firestore.batch()
                snapshot.documents.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
            userRef.collection("settings").document("app").delete().await()
            deleteAllUserTabFiles(user.uid)
            deleteAllAudioNoteFiles(user.uid)
            Log.w(LogTag, "clearRemoteData:success uid=${user.uid}")
            Unit
        }.also {
            syncingState.value = false
        }
    }

    override suspend fun clearLocalUserData(): Result<Unit> {
        return runCatching {
            clearLocalCloudScopedData()
            ensureBuiltInTabsSeeded()
            appSettingsRepository.setSyncOwnerUid(null)
            appSettingsRepository.setLastCloudSyncAt(null)
        }
    }

    private suspend fun clearLocalCloudScopedData() {
        Log.w(LogTag, "clearLocalCloudScopedData:start")
        deleteLocalAudioNoteFiles(audioNoteDao.getAllNotes())
        tabRepository.clearAllTabs()
        sessionRepository.clearHistory()
        goalRepository.clearGoals()
        progressRepository.clearAll()
        audioNoteDao.clearAll()
        textNoteDao.clearAll()
        appSettingsRepository.resetSettingsToDefaults()
        appSettingsRepository.clearAllPendingDeletedUserTabIds()
        Log.w(LogTag, "clearLocalCloudScopedData:done")
    }

    private suspend fun ensureBuiltInTabsSeeded() {
        tabRepository.getTabs().first()
    }

    private fun mergeSettings(
        local: AppSettingsSnapshot,
        remote: AppSettingsSnapshot?
    ): AppSettingsSnapshot {
        if (remote == null) return local
        return if (remote.updatedAt > local.updatedAt) remote else local
    }

    private data class MergeTabsResult(
        val tabs: List<TabItem>,
        val tabsToDelete: List<TabItem>
    )

    private fun mergeTabCollections(
        localTabs: List<TabItem>,
        remoteTabs: List<TabItem>
    ): MergeTabsResult {
        val mergedTabs = mutableListOf<TabItem>()
        val localTabsToDelete = mutableListOf<TabItem>()
        val matchedLocalIds = mutableSetOf<String>()

        remoteTabs.forEach { remote ->
            val localById = localTabs.firstOrNull { it.id == remote.id }
            if (localById != null) {
                matchedLocalIds += localById.id
                mergedTabs += mergeTabs(localById, remote)
            } else {
                val localByKey = localTabs.firstOrNull {
                    it.id !in matchedLocalIds && canonicalTabKey(it) == canonicalTabKey(remote)
                }
                if (localByKey != null) {
                    matchedLocalIds += localByKey.id
                    if (localByKey.id != remote.id && localByKey.isUserTab) {
                        localTabsToDelete += localByKey
                    }
                    mergedTabs += mergeTabs(localByKey, remote)
                } else {
                    mergedTabs += remote
                }
            }
        }

        localTabs.forEach { local ->
            val hasRemoteById = remoteTabs.any { it.id == local.id }
            val hasRemoteByKey = remoteTabs.any { canonicalTabKey(it) == canonicalTabKey(local) }
            if (!hasRemoteById && !hasRemoteByKey && local.id !in matchedLocalIds) {
                mergedTabs += local
            }
        }

        val deduped = mergedTabs
            .groupBy { canonicalTabIdentity(it) }
            .map { (_, variants) -> variants.reduce(::mergeTabs) }

        return MergeTabsResult(deduped, localTabsToDelete.distinctBy { it.id })
    }

    private fun mergeTabs(local: TabItem, remote: TabItem): TabItem {
        val newer = if (local.updatedAt >= remote.updatedAt) local else remote
        val older = if (newer === local) remote else local

        return newer.copy(
            id = remote.id.ifBlank { local.id },
            name = newer.name.ifBlank { older.name },
            description = newer.description.ifBlank { older.description },
            difficulty = newer.difficulty,
            lessonNumber = if (newer.lessonNumber != 0) newer.lessonNumber else older.lessonNumber,
            isCompleted = if (local.updatedAt == remote.updatedAt) {
                local.isCompleted || remote.isCompleted
            } else {
                newer.isCompleted
            },
            isUserTab = local.isUserTab || remote.isUserTab,
            filePath = newer.filePath ?: older.filePath,
            asciiTabs = newer.asciiTabs ?: older.asciiTabs,
            tagsCsv = mergeTags(local.tagsCsv, remote.tagsCsv),
            folder = newer.folder.takeIf { it.isNotBlank() } ?: older.folder,
            openCount = maxOf(local.openCount, remote.openCount),
            lastOpenedAt = maxOf(local.lastOpenedAt, remote.lastOpenedAt),
            createdAt = listOf(local.createdAt, remote.createdAt)
                .filter { it > 0L }
                .minOrNull()
                ?: 0L,
            updatedAt = maxOf(local.updatedAt, remote.updatedAt),
            offlineReady = local.offlineReady || remote.offlineReady
        )
    }

    private fun mergeGoals(localGoals: List<Goal>, remoteGoals: List<Goal>): List<Goal> {
        return (localGoals + remoteGoals)
            .groupBy { it.syncId.ifBlank { goalFingerprint(it) } }
            .map { (_, variants) -> variants.maxByOrNull { it.updatedAt } ?: variants.first() }
            .sortedBy { it.deadline }
    }

    private fun mergeProgress(
        localProgress: List<TabPlaybackProgress>,
        remoteProgress: List<TabPlaybackProgress>
    ): List<TabPlaybackProgress> {
        return (localProgress + remoteProgress)
            .groupBy { it.tabId }
            .map { (_, variants) -> variants.maxByOrNull { it.updatedAt } ?: variants.first() }
    }

    private fun mergeTextNotes(
        localNotes: List<TextNote>,
        remoteNotes: List<TextNote>
    ): List<TextNote> {
        return (localNotes + remoteNotes)
            .groupBy(::textNoteDocumentId)
            .map { (_, variants) ->
                variants.reduce { current, candidate ->
                    val preferredContent = when {
                        current.content == candidate.content -> current.content
                        current.content.isBlank() -> candidate.content
                        candidate.content.isBlank() -> current.content
                        else -> current.content
                    }
                    current.copy(
                        content = preferredContent,
                        isFavorite = current.isFavorite || candidate.isFavorite,
                        createdAt = if (current.createdAt.time <= candidate.createdAt.time) {
                            current.createdAt
                        } else {
                            candidate.createdAt
                        }
                    )
                }
            }
            .sortedByDescending { it.createdAt.time }
    }

    private fun mergeAudioNotes(
        localNotes: List<AudioNote>,
        remoteNotes: List<AudioNote>
    ): List<AudioNote> {
        return (localNotes + remoteNotes)
            .groupBy(::audioNoteDocumentId)
            .map { (_, variants) ->
                variants.reduce { current, candidate ->
                    val currentFileExists = File(current.filePath).exists()
                    val candidateFileExists = File(candidate.filePath).exists()
                    current.copy(
                        filePath = when {
                            currentFileExists -> current.filePath
                            candidateFileExists -> candidate.filePath
                            current.filePath.isNotBlank() -> current.filePath
                            else -> candidate.filePath
                        },
                        isFavorite = current.isFavorite || candidate.isFavorite,
                        createdAt = if (current.createdAt.time <= candidate.createdAt.time) {
                            current.createdAt
                        } else {
                            candidate.createdAt
                        }
                    )
                }
            }
            .sortedByDescending { it.createdAt.time }
    }

    private fun deleteStaleDocuments(
        batch: com.google.firebase.firestore.WriteBatch,
        parent: com.google.firebase.firestore.CollectionReference,
        remoteIds: Set<String>,
        localIds: Set<String>
    ) {
        (remoteIds - localIds).forEach { staleId ->
            batch.delete(parent.document(staleId))
        }
    }

    private fun AppSettingsSnapshot.toFirestoreMap(): Map<String, Any> {
        return mapOf(
            "themeMode" to themeMode.name,
            "appLanguage" to appLanguage.name,
            "aiProvider" to aiProvider.name,
            "localAiServerUrl" to localAiServerUrl,
            "normalSpeed" to normalSpeed,
            "practiceSpeed" to practiceSpeed,
            "normalTabScale" to normalTabScale,
            "practiceTabScale" to practiceTabScale,
            "tabDisplayMode" to tabDisplayMode.name,
            "fretboardDisplayMode" to fretboardDisplayMode.name,
            "updatedAt" to updatedAt
        )
    }

    private fun TabItem.toFirestoreMap(filePayload: CloudTabFilePayload?): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "name" to name,
            "description" to description,
            "difficulty" to difficulty.name,
            "lessonNumber" to lessonNumber,
            "isCompleted" to isCompleted,
            "isUserTab" to isUserTab,
            "filePath" to filePath,
            "storagePath" to filePayload?.storagePath,
            "fileBase64" to filePayload?.fileBase64,
            "asciiTabs" to asciiTabs,
            "tagsCsv" to tagsCsv,
            "folder" to normalizeTabFolder(folder),
            "openCount" to openCount,
            "lastOpenedAt" to lastOpenedAt,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "offlineReady" to offlineReady
        )
    }

    private fun Session.toFirestoreMap(): Map<String, Any> {
        return mapOf(
            "startTime" to startTime.time,
            "endTime" to endTime.time,
            "duration" to duration,
            "practicedTabs" to practicedTabs.map { it.toFirestoreMap() }
        )
    }

    private fun TextNote.toFirestoreMap(): Map<String, Any> {
        return mapOf(
            "lessonId" to lessonId,
            "content" to content,
            "createdAt" to createdAt.time,
            "isFavorite" to isFavorite
        )
    }

    private fun AudioNote.toFirestoreMap(filePayload: CloudAudioNoteFilePayload?): Map<String, Any?> {
        return mapOf(
            "lessonId" to lessonId,
            "fileName" to File(filePath).name,
            "storagePath" to filePayload?.storagePath,
            "fileBase64" to filePayload?.fileBase64,
            "createdAt" to createdAt.time,
            "isFavorite" to isFavorite
        )
    }

    private fun AudioNote.toFirestoreBackupMap(
        documentId: String,
        filePayload: CloudAudioNoteFilePayload?
    ): Map<String, Any?> {
        return toFirestoreMap(filePayload).toMutableMap().apply {
            put("documentId", documentId)
        }
    }

    private fun PracticedTab.toFirestoreMap(): Map<String, Any> {
        return mapOf(
            "tabId" to tabId,
            "tabName" to tabName,
            "duration" to duration
        )
    }

    private fun Goal.toFirestoreMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "syncId" to syncId,
            "type" to type.name,
            "description" to description,
            "target" to target,
            "progress" to progress,
            "deadline" to deadline,
            "updatedAt" to updatedAt,
            "isCompleted" to isCompleted,
            "isOverdue" to isOverdue
        )
    }

    private fun TabPlaybackProgress.toFirestoreMap(): Map<String, Any> {
        return mapOf(
            "tabId" to tabId,
            "tabName" to tabName,
            "lastTick" to lastTick,
            "lastBarIndex" to lastBarIndex,
            "totalBars" to totalBars,
            "updatedAt" to updatedAt
        )
    }

    private fun DocumentSnapshot.toSettings(): AppSettingsSnapshot? {
        if (!exists()) return null
        return AppSettingsSnapshot(
            themeMode = enumValueOrDefault(getString("themeMode"), AppSettingsSnapshot().themeMode),
            appLanguage = enumValueOrDefault(getString("appLanguage"), AppSettingsSnapshot().appLanguage),
            aiProvider = enumValueOrDefault(getString("aiProvider"), AppSettingsSnapshot().aiProvider),
            localAiServerUrl = getString("localAiServerUrl").orEmpty(),
            normalSpeed = getDouble("normalSpeed")?.toFloat() ?: 1.0f,
            practiceSpeed = getDouble("practiceSpeed")?.toFloat() ?: 0.25f,
            normalTabScale = getDouble("normalTabScale")?.toFloat() ?: 1.0f,
            practiceTabScale = getDouble("practiceTabScale")?.toFloat() ?: 1.0f,
            tabDisplayMode = enumValueOrDefault(getString("tabDisplayMode"), AppSettingsSnapshot().tabDisplayMode),
            fretboardDisplayMode = enumValueOrDefault(
                getString("fretboardDisplayMode"),
                AppSettingsSnapshot().fretboardDisplayMode
            ),
            updatedAt = getLong("updatedAt") ?: 0L
        )
    }

    private fun DocumentSnapshot.toSession(): Session? {
        return runCatching {
            val startTime = getDateCompat("startTime") ?: return null
            val endTime = getDateCompat("endTime") ?: return null
            val duration = getLongCompat("duration") ?: 0L
            val practicedTabsRaw = get("practicedTabs") as? List<*> ?: emptyList<Any>()
            val practicedTabs = practicedTabsRaw.mapNotNull { item ->
                val map = item as? Map<*, *> ?: return@mapNotNull null
                PracticedTab(
                    tabId = map["tabId"]?.toString().orEmpty(),
                    tabName = map["tabName"]?.toString().orEmpty(),
                    duration = map.getLongCompat("duration") ?: 0L
                )
            }
            Session(
                startTime = startTime,
                endTime = endTime,
                duration = duration,
                practicedTabs = practicedTabs
            )
        }.onFailure { error ->
            Log.w(LogTag, "toSession:unreadable docId=$id data=${data.orEmpty()}", error)
        }.getOrNull()
    }

    private fun DocumentSnapshot.toTextNote(): TextNote? {
        return runCatching {
            val lessonId = getStringCompat("lessonId") ?: return null
            val createdAt = getDateCompat("createdAt") ?: return null
            TextNote(
                lessonId = lessonId,
                content = getStringCompat("content").orEmpty(),
                createdAt = createdAt,
                isFavorite = getBooleanCompat("isFavorite") ?: false
            )
        }.onFailure { error ->
            Log.w(LogTag, "toTextNote:unreadable docId=$id data=${data.orEmpty()}", error)
        }.getOrNull()
    }

    private fun DocumentSnapshot.toSessionBackups(): List<Session> {
        val rawItems = get("sessionBackups") as? List<*> ?: return emptyList()
        return rawItems.mapNotNull { (it as? Map<*, *>)?.toSessionBackup() }
    }

    private fun DocumentSnapshot.toTextNoteBackups(): List<TextNote> {
        val rawItems = get("textNoteBackups") as? List<*> ?: return emptyList()
        return rawItems.mapNotNull { (it as? Map<*, *>)?.toTextNoteBackup() }
    }

    private suspend fun DocumentSnapshot.toAudioNoteBackups(): List<AudioNote> {
        val rawItems = get("audioNoteBackups") as? List<*> ?: return emptyList()
        return rawItems.mapNotNull { (it as? Map<*, *>)?.toAudioNoteBackup() }
    }

    private suspend fun DocumentSnapshot.toAudioNote(): AudioNote? {
        return runCatching {
            val lessonId = getStringCompat("lessonId") ?: return null
            val createdAt = getDateCompat("createdAt") ?: return null
            val restoredPath = restoreAudioNoteFile(
                documentId = id,
                fileName = getStringCompat("fileName"),
                storagePath = getStringCompat("storagePath"),
                fileBase64 = getStringCompat("fileBase64")
            ) ?: return null
            AudioNote(
                lessonId = lessonId,
                filePath = restoredPath,
                createdAt = createdAt,
                isFavorite = getBooleanCompat("isFavorite") ?: false
            )
        }.onFailure { error ->
            Log.w(LogTag, "toAudioNote:unreadable docId=$id data=${data.orEmpty()}", error)
        }.getOrNull()
    }

    private fun Map<*, *>.toSessionBackup(): Session? {
        return runCatching {
            val startTime = getDateCompat("startTime") ?: return null
            val endTime = getDateCompat("endTime") ?: return null
            val duration = (this["duration"] as? Number)?.toLong() ?: 0L
            val practicedTabsRaw = this["practicedTabs"] as? List<*> ?: emptyList<Any>()
            val practicedTabs = practicedTabsRaw.mapNotNull { item ->
                val map = item as? Map<*, *> ?: return@mapNotNull null
                PracticedTab(
                    tabId = map["tabId"] as? String ?: "",
                    tabName = map["tabName"] as? String ?: "",
                    duration = (map["duration"] as? Number)?.toLong() ?: 0L
                )
            }
            Session(
                startTime = startTime,
                endTime = endTime,
                duration = duration,
                practicedTabs = practicedTabs
            )
        }.getOrNull()
    }

    private fun Map<*, *>.toTextNoteBackup(): TextNote? {
        return runCatching {
            val lessonId = this["lessonId"] as? String ?: return null
            val createdAt = getDateCompat("createdAt") ?: return null
            TextNote(
                lessonId = lessonId,
                content = this["content"] as? String ?: "",
                createdAt = createdAt,
                isFavorite = this["isFavorite"] as? Boolean ?: false
            )
        }.getOrNull()
    }

    private suspend fun Map<*, *>.toAudioNoteBackup(): AudioNote? {
        return runCatching {
            val lessonId = this["lessonId"] as? String ?: return null
            val createdAt = getDateCompat("createdAt") ?: return null
            val documentId = this["documentId"] as? String ?: audioNoteDocumentId(
                AudioNote(
                    lessonId = lessonId,
                    filePath = "",
                    createdAt = createdAt
                )
            )
            val restoredPath = restoreAudioNoteFile(
                documentId = documentId,
                fileName = this["fileName"] as? String,
                storagePath = this["storagePath"] as? String,
                fileBase64 = this["fileBase64"] as? String
            ) ?: return null
            AudioNote(
                lessonId = lessonId,
                filePath = restoredPath,
                createdAt = createdAt,
                isFavorite = this["isFavorite"] as? Boolean ?: false
            )
        }.getOrNull()
    }

    private suspend fun DocumentSnapshot.toTabItem(): TabItem? {
        return runCatching {
            val isUserTab = getBoolean("isUserTab") ?: false
            val originalPath = getString("filePath")
            val localPath = if (isUserTab) {
                restoreUserTabFile(
                    tabId = getString("id") ?: id,
                    tabName = getString("name") ?: "",
                    originalPath = originalPath,
                    storagePath = getString("storagePath"),
                    fileBase64 = getString("fileBase64")
                )
            } else {
                originalPath
            }

            if (isUserTab && localPath.isNullOrBlank()) {
                return null
            }

            TabItem(
                id = getString("id") ?: id,
                name = getString("name") ?: "",
                description = getString("description") ?: "",
                difficulty = enumValueOrDefault(getString("difficulty"), Difficulty.BEGINNER),
                lessonNumber = getLong("lessonNumber")?.toInt() ?: 0,
                isCompleted = getBoolean("isCompleted") ?: false,
                isUserTab = isUserTab,
                filePath = localPath,
                asciiTabs = getString("asciiTabs"),
                tagsCsv = getString("tagsCsv") ?: "",
                folder = normalizeTabFolder(getString("folder") ?: DEFAULT_TAB_FOLDER_KEY),
                openCount = getLong("openCount")?.toInt() ?: 0,
                lastOpenedAt = getLong("lastOpenedAt") ?: 0L,
                createdAt = getLong("createdAt")
                    ?: getLong("updatedAt")
                    ?: getLong("lastOpenedAt")
                    ?: 0L,
                updatedAt = getLong("updatedAt") ?: 0L,
                offlineReady = getBoolean("offlineReady") ?: false
            )
        }.getOrNull()
    }

    private fun DocumentSnapshot.toGoal(): Goal? {
        return runCatching {
            Goal(
                id = getLong("id")?.toInt() ?: 0,
                syncId = getString("syncId") ?: id,
                type = enumValueOrDefault(getString("type"), GoalType.CUSTOM),
                description = getString("description") ?: "",
                target = getLong("target")?.toInt() ?: 0,
                progress = getLong("progress")?.toInt() ?: 0,
                deadline = getLong("deadline") ?: 0L,
                updatedAt = getLong("updatedAt") ?: 0L,
                isCompleted = getBoolean("isCompleted") ?: false,
                isOverdue = getBoolean("isOverdue") ?: false
            )
        }.getOrNull()
    }

    private fun DocumentSnapshot.toProgress(): TabPlaybackProgress? {
        return runCatching {
            TabPlaybackProgress(
                tabId = getString("tabId") ?: id,
                tabName = getString("tabName") ?: "",
                lastTick = getLong("lastTick") ?: 0L,
                lastBarIndex = getLong("lastBarIndex")?.toInt() ?: 0,
                totalBars = getLong("totalBars")?.toInt() ?: 0,
                updatedAt = getLong("updatedAt") ?: 0L
            )
        }.getOrNull()
    }

    private fun canonicalTabName(name: String): String {
        return name.trim().lowercase().replace(Regex("\\s+"), " ")
    }

    private fun canonicalTabKey(tab: TabItem): String {
        return "${tab.isUserTab}|${canonicalTabName(tab.name)}|${tab.lessonNumber}"
    }

    private fun canonicalTabIdentity(tab: TabItem): String {
        return if (tab.isUserTab) canonicalTabKey(tab) else "builtin:${tab.id}"
    }

    private fun mergeTags(local: String, remote: String): String {
        return (local.split(",") + remote.split(","))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString(",")
    }

    private suspend fun importRemoteTabs(documents: List<DocumentSnapshot>): RemoteTabsImportResult {
        val tabs = mutableListOf<TabItem>()
        val unresolvedRemoteIds = mutableSetOf<String>()

        documents.forEach { document ->
            val remoteId = document.getString("id") ?: document.id
            val importedTab = document.toTabItem()
            if (importedTab != null) {
                tabs += importedTab
            } else if (document.getBoolean("isUserTab") == true) {
                unresolvedRemoteIds += remoteId
            }
        }

        return RemoteTabsImportResult(
            tabs = tabs,
            unresolvedRemoteIds = unresolvedRemoteIds
        )
    }

    private fun importRemoteSessions(documents: List<DocumentSnapshot>): RemoteImportResult<Session> {
        val sessions = mutableListOf<Session>()
        val unresolvedRemoteIds = mutableSetOf<String>()

        documents.forEach { document ->
            val session = document.toSession()
            if (session != null) {
                sessions += session
            } else {
                unresolvedRemoteIds += document.id
            }
        }

        return RemoteImportResult(
            items = sessions,
            unresolvedRemoteIds = unresolvedRemoteIds
        )
    }

    private fun importRemoteTextNotes(documents: List<DocumentSnapshot>): RemoteImportResult<TextNote> {
        val textNotes = mutableListOf<TextNote>()
        val unresolvedRemoteIds = mutableSetOf<String>()

        documents.forEach { document ->
            val textNote = document.toTextNote()
            if (textNote != null) {
                textNotes += textNote
            } else {
                unresolvedRemoteIds += document.id
            }
        }

        return RemoteImportResult(
            items = textNotes,
            unresolvedRemoteIds = unresolvedRemoteIds
        )
    }

    private suspend fun importRemoteAudioNotes(documents: List<DocumentSnapshot>): RemoteImportResult<AudioNote> {
        val audioNotes = mutableListOf<AudioNote>()
        val unresolvedRemoteIds = mutableSetOf<String>()

        documents.forEach { document ->
            val audioNote = document.toAudioNote()
            if (audioNote != null) {
                audioNotes += audioNote
            } else {
                unresolvedRemoteIds += document.id
            }
        }

        return RemoteImportResult(
            items = audioNotes,
            unresolvedRemoteIds = unresolvedRemoteIds
        )
    }

    private suspend fun restoreUserTabFile(
        tabId: String,
        tabName: String,
        originalPath: String?,
        storagePath: String?,
        fileBase64: String?
    ): String? {
        val existing = originalPath?.takeIf { path ->
            runCatching { File(path).exists() }.getOrDefault(false)
        }
        if (existing != null) return existing

        val safeName = tabName
            .ifBlank { tabId }
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .take(48)
        val targetDir = File(context.filesDir, "synced_tabs").apply { mkdirs() }
        val targetFile = File(targetDir, "${tabId}_$safeName.gp")

        if (!storagePath.isNullOrBlank()) {
            val restoredFromStorage = runCatching {
                if (!targetFile.exists() || targetFile.length() == 0L) {
                    storage.reference.child(storagePath).getFile(targetFile).await()
                }
                targetFile.absolutePath
            }.getOrNull()
            if (restoredFromStorage != null) return restoredFromStorage
        }

        if (fileBase64.isNullOrBlank()) return null

        return runCatching {
            if (!targetFile.exists() || targetFile.length() == 0L) {
                targetFile.writeBytes(Base64.decode(fileBase64, Base64.DEFAULT))
            }
            targetFile.absolutePath
        }.getOrNull()
    }

    private suspend fun restoreAudioNoteFile(
        documentId: String,
        fileName: String?,
        storagePath: String?,
        fileBase64: String?
    ): String? {
        val safeName = fileName
            ?.ifBlank { null }
            ?.replace(Regex("[^A-Za-z0-9._-]"), "_")
            ?.take(64)
            ?: documentId
        val extension = safeName.substringAfterLast('.', "").takeIf { it.isNotBlank() }
        val targetDir = File(context.filesDir, "synced_audio_notes").apply { mkdirs() }
        val targetFile = if (extension != null) {
            File(targetDir, "${documentId}_$safeName")
        } else {
            File(targetDir, documentId)
        }

        if (!storagePath.isNullOrBlank()) {
            val restoredFromStorage = runCatching {
                if (!targetFile.exists() || targetFile.length() == 0L) {
                    storage.reference.child(storagePath).getFile(targetFile).await()
                }
                targetFile.absolutePath
            }.getOrNull()
            if (restoredFromStorage != null) return restoredFromStorage
        }

        if (fileBase64.isNullOrBlank()) return null

        return runCatching {
            if (!targetFile.exists() || targetFile.length() == 0L) {
                targetFile.writeBytes(Base64.decode(fileBase64, Base64.DEFAULT))
            }
            targetFile.absolutePath
        }.getOrNull()
    }

    private suspend fun uploadUserTabFile(userId: String, tab: TabItem): String? {
        if (!tab.isUserTab) return null
        val path = tab.filePath ?: return null
        val file = File(path)
        if (!file.exists() || !file.isFile) return null

        val storagePath = storagePathForTab(userId, tab.id)
        return runCatching {
            storage.reference.child(storagePath)
                .putFile(Uri.fromFile(file))
                .await()
            storagePath
        }.getOrNull()
    }

    private suspend fun prepareCloudTabFilePayload(
        userId: String,
        tab: TabItem,
        existingStoragePath: String?
    ): CloudTabFilePayload {
        if (!tab.isUserTab) return CloudTabFilePayload(storagePath = null, fileBase64 = null)

        val uploadedStoragePath = uploadUserTabFile(userId, tab)
        if (!uploadedStoragePath.isNullOrBlank()) {
            return CloudTabFilePayload(
                storagePath = uploadedStoragePath,
                fileBase64 = null
            )
        }

        val verifiedExistingStoragePath = existingStoragePath?.takeIf { path ->
            path.isNotBlank() && isStorageObjectAvailable(path)
        }
        val inlineFileBase64 = exportUserTabFileBase64(tab)

        if (!inlineFileBase64.isNullOrBlank()) {
            return CloudTabFilePayload(
                storagePath = verifiedExistingStoragePath,
                fileBase64 = inlineFileBase64
            )
        }

        if (!verifiedExistingStoragePath.isNullOrBlank()) {
            return CloudTabFilePayload(
                storagePath = verifiedExistingStoragePath,
                fileBase64 = null
            )
        }

        throw IllegalStateException(
            context.getString(com.guitarlearning.R.string.sync_error_tab_file_failed, tab.name)
        )
    }

    private suspend fun uploadAudioNoteFile(
        userId: String,
        documentId: String,
        audioNote: AudioNote
    ): String? {
        val file = File(audioNote.filePath)
        if (!file.exists() || !file.isFile) return null

        val storagePath = storagePathForAudioNote(
            userId = userId,
            documentId = documentId,
            extension = file.extension
        )
        return runCatching {
            storage.reference.child(storagePath)
                .putFile(Uri.fromFile(file))
                .await()
            storagePath
        }.getOrNull()
    }

    private suspend fun prepareCloudAudioNoteFilePayload(
        userId: String,
        documentId: String,
        audioNote: AudioNote,
        existingStoragePath: String?
    ): CloudAudioNoteFilePayload {
        val uploadedStoragePath = uploadAudioNoteFile(userId, documentId, audioNote)
        if (!uploadedStoragePath.isNullOrBlank()) {
            return CloudAudioNoteFilePayload(
                storagePath = uploadedStoragePath,
                fileBase64 = null
            )
        }

        val verifiedExistingStoragePath = existingStoragePath?.takeIf { path ->
            path.isNotBlank() && isStorageObjectAvailable(path)
        }
        val inlineFileBase64 = exportAudioNoteFileBase64(audioNote)

        if (!inlineFileBase64.isNullOrBlank()) {
            return CloudAudioNoteFilePayload(
                storagePath = verifiedExistingStoragePath,
                fileBase64 = inlineFileBase64
            )
        }

        if (!verifiedExistingStoragePath.isNullOrBlank()) {
            return CloudAudioNoteFilePayload(
                storagePath = verifiedExistingStoragePath,
                fileBase64 = null
            )
        }

        throw IllegalStateException("Failed to sync audio note file for lesson ${audioNote.lessonId}")
    }

    private fun exportUserTabFileBase64(tab: TabItem): String? {
        if (!tab.isUserTab) return null
        val path = tab.filePath ?: return null
        val file = File(path)
        if (!file.exists() || !file.isFile) return null
        if (file.length() > MaxInlineUserTabBytes) return null
        return runCatching {
            Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
        }.getOrNull()
    }

    private fun exportAudioNoteFileBase64(audioNote: AudioNote): String? {
        val file = File(audioNote.filePath)
        if (!file.exists() || !file.isFile) return null
        if (file.length() > MaxInlineAudioNoteBytes) return null
        return runCatching {
            Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
        }.getOrNull()
    }

    private suspend fun isStorageObjectAvailable(storagePath: String): Boolean {
        return runCatching {
            storage.reference.child(storagePath).metadata.await()
            true
        }.getOrDefault(false)
    }

    private suspend fun deleteUserTabFile(userId: String, tabId: String) {
        runCatching {
            storage.reference.child(storagePathForTab(userId, tabId)).delete().await()
        }
    }

    private suspend fun deleteAudioNoteFile(storagePath: String?) {
        if (storagePath.isNullOrBlank()) return
        runCatching {
            storage.reference.child(storagePath).delete().await()
        }
    }

    private suspend fun deleteStaleUserTabFiles(
        userId: String,
        remoteIds: Set<String>,
        localIds: Set<String>
    ) {
        (remoteIds - localIds).forEach { staleId ->
            deleteUserTabFile(userId, staleId)
        }
    }

    private suspend fun deleteStaleAudioNoteFiles(
        userId: String,
        remoteIds: Set<String>,
        localIds: Set<String>,
        remoteStoragePathsById: Map<String, String?>
    ) {
        (remoteIds - localIds).forEach { staleId ->
            val storagePath = remoteStoragePathsById[staleId]
                ?: storagePathForAudioNote(userId, staleId, extension = null)
            deleteAudioNoteFile(storagePath)
        }
    }

    private suspend fun deleteAllUserTabFiles(userId: String) {
        runCatching {
            val listResult = storage.reference.child("users/$userId/tabs").listAll().await()
            listResult.items.forEach { item ->
                runCatching { item.delete().await() }
            }
        }
    }

    private suspend fun deleteAllAudioNoteFiles(userId: String) {
        runCatching {
            val listResult = storage.reference.child("users/$userId/audio_notes").listAll().await()
            listResult.items.forEach { item ->
                runCatching { item.delete().await() }
            }
        }
    }

    private fun deleteLocalAudioNoteFiles(audioNotes: List<AudioNote>) {
        audioNotes.forEach { audioNote ->
            runCatching {
                File(audioNote.filePath).takeIf { it.exists() }?.delete()
            }
        }
        runCatching {
            File(context.filesDir, "synced_audio_notes")
                .takeIf { it.exists() }
                ?.listFiles()
                ?.forEach { it.delete() }
        }
    }

    private fun sessionDocumentId(session: Session): String {
        return sha1(
            buildString {
                append(session.startTime.time)
                append('|')
                append(session.endTime.time)
                append('|')
                append(session.duration)
                append('|')
                append(
                    session.practicedTabs.joinToString(";") {
                        "${it.tabId}:${it.tabName}:${it.duration}"
                    }
                )
            }
        )
    }

    private fun textNoteDocumentId(textNote: TextNote): String {
        return sha1("${textNote.lessonId}|${textNote.createdAt.time}")
    }

    private fun audioNoteDocumentId(audioNote: AudioNote): String {
        return sha1("${audioNote.lessonId}|${audioNote.createdAt.time}")
    }

    private fun goalFingerprint(goal: Goal): String {
        return sha1(
            buildString {
                append(goal.type.name)
                append('|')
                append(goal.description)
                append('|')
                append(goal.target)
                append('|')
                append(goal.deadline)
            }
        )
    }

    private fun sha1(value: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun storagePathForTab(userId: String, tabId: String): String {
        return "users/$userId/tabs/$tabId.gp"
    }

    private fun storagePathForAudioNote(
        userId: String,
        documentId: String,
        extension: String?
    ): String {
        val normalizedExtension = extension
            ?.trim()
            ?.trimStart('.')
            ?.takeIf { it.isNotBlank() }
            ?.let { ".$it" }
            .orEmpty()
        return "users/$userId/audio_notes/$documentId$normalizedExtension"
    }

    private fun DocumentSnapshot.getDateCompat(field: String): Date? {
        return when (val value = get(field)) {
            is Date -> value
            is Timestamp -> value.toDate()
            is Number -> Date(value.toLong())
            is String -> value.toLongOrNull()?.let(::Date)
            else -> getDate(field) ?: getLong(field)?.let(::Date)
        }
    }

    private fun DocumentSnapshot.getLongCompat(field: String): Long? {
        return when (val value = get(field)) {
            is Number -> value.toLong()
            is String -> value.toLongOrNull()
            is Date -> value.time
            is Timestamp -> value.toDate().time
            else -> getLong(field)
        }
    }

    private fun DocumentSnapshot.getStringCompat(field: String): String? {
        return when (val value = get(field)) {
            null -> getString(field)
            is String -> value
            else -> value.toString()
        }
    }

    private fun DocumentSnapshot.getBooleanCompat(field: String): Boolean? {
        return when (val value = get(field)) {
            is Boolean -> value
            is Number -> value.toInt() != 0
            is String -> value.equals("true", ignoreCase = true) || value == "1"
            else -> getBoolean(field)
        }
    }

    private fun Map<*, *>.getDateCompat(field: String): Date? {
        return when (val value = this[field]) {
            is Date -> value
            is Timestamp -> value.toDate()
            is Number -> Date(value.toLong())
            is String -> value.toLongOrNull()?.let(::Date)
            else -> null
        }
    }

    private fun Map<*, *>.getLongCompat(field: String): Long? {
        return when (val value = this[field]) {
            is Number -> value.toLong()
            is String -> value.toLongOrNull()
            is Date -> value.time
            is Timestamp -> value.toDate().time
            else -> null
        }
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, fallback: T): T {
        return value?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: fallback
    }
}
