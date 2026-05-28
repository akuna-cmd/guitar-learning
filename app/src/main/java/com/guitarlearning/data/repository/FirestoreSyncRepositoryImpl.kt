package com.guitarlearning.data.repository

import android.content.Context
import android.util.Log
import com.guitarlearning.data.local.AudioNoteDao
import com.guitarlearning.data.local.TextNoteDao
import com.guitarlearning.data.settings.AppSettingsRepository
import com.guitarlearning.domain.model.AudioNote
import com.guitarlearning.domain.model.Goal
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.io.File
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
    storage: FirebaseStorage
) : SyncRepository {
    private companion object {
        const val LogTag = "CloudSync"
        const val MaxFirestoreBatchOps = 450
    }

    private val syncingState = MutableStateFlow(false)
    private val fileStore = FirestoreSyncFileStore(context, storage)
    private val mapper = FirestoreSyncMapper(
        logTag = LogTag,
        restoreUserTabFile = fileStore::restoreUserTabFile,
        restoreAudioNoteFile = fileStore::restoreAudioNoteFile
    )
    private val mergePolicy = FirestoreSyncMergePolicy(mapper)

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
            fileStore.deleteUserTabFile(user.uid, tab.id)
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
            val storedPendingDeletedUserTabIds = appSettingsRepository.getPendingDeletedUserTabIds()
            val isAccountSwitch = !previousOwnerUid.isNullOrBlank() && previousOwnerUid != user.uid
            val preferRemoteState = previousOwnerUid.isNullOrBlank() || isAccountSwitch
            val pendingDeletedUserTabIds = if (preferRemoteState) {
                emptySet()
            } else {
                storedPendingDeletedUserTabIds
            }

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
            val remoteSettings = mapper.toSettings(remoteSettingsDocument)
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
            val remoteTabsImport = mapper.importRemoteTabs(filteredRemoteTabDocuments)
            val remoteTabs = remoteTabsImport.tabs
            val settingsSessionBackups = mapper.toSessionBackups(remoteSettingsDocument)
            val settingsTextNoteBackups = mapper.toTextNoteBackups(remoteSettingsDocument)
            val settingsAudioNoteBackups = mapper.toAudioNoteBackups(remoteSettingsDocument)
            val usedSessionFallback = remoteSessionsSnapshot.isEmpty
            val remoteSessionsImport = if (usedSessionFallback) {
                RemoteImportResult(
                    items = settingsSessionBackups,
                    unresolvedRemoteIds = emptySet()
                )
            } else {
                mapper.importRemoteSessions(remoteSessionsSnapshot.documents)
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
            val remoteGoals = remoteGoalsSnapshot.documents.mapNotNull(mapper::toGoal)
            val remoteProgress = remoteProgressSnapshot.documents.mapNotNull(mapper::toProgress)
            val usedTextNoteFallback = remoteTextNotesSnapshot.isEmpty
            val remoteTextNotesImport = if (usedTextNoteFallback) {
                RemoteImportResult(
                    items = settingsTextNoteBackups,
                    unresolvedRemoteIds = emptySet()
                )
            } else {
                mapper.importRemoteTextNotes(remoteTextNotesSnapshot.documents)
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
                mapper.importRemoteAudioNotes(remoteAudioNotesSnapshot.documents)
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
            val mergedSettings = mergePolicy.mergeSettings(
                local = localSettings,
                remote = remoteSettings,
                preferRemote = preferRemoteState
            )
            appSettingsRepository.replaceSettings(mergedSettings)

            val localTabs = tabRepository.getAllTabsSync()
            val mergedTabsResult = mergePolicy.mergeTabCollections(localTabs, remoteTabs)
            if (mergedTabsResult.tabsToDelete.isNotEmpty()) {
                tabRepository.deleteTabs(mergedTabsResult.tabsToDelete)
            }
            if (mergedTabsResult.tabs.isNotEmpty()) {
                tabRepository.upsertTabs(mergedTabsResult.tabs)
            }

            sessionRepository.importHistory(remoteSessions)

            val localGoals = goalRepository.getGoalsSync()
            val mergedGoals = mergePolicy.mergeGoals(localGoals, remoteGoals)
            goalRepository.clearGoals()
            if (mergedGoals.isNotEmpty()) {
                goalRepository.upsertGoals(mergedGoals)
            }

            val localProgress = progressRepository.observeAll().first()
            val mergedProgress = mergePolicy.mergeProgress(localProgress, remoteProgress)
            progressRepository.replaceAll(mergedProgress)

            val localTextNotes = textNoteDao.getAllTextNotes()
            val useRemoteNotesAsSourceOfTruth = preferRemoteState
            val mergedTextNotes = if (useRemoteNotesAsSourceOfTruth) {
                mergePolicy.mergeTextNotes(localTextNotes, remoteTextNotes)
            } else {
                localTextNotes.sortedByDescending { it.createdAt.time }
            }
            textNoteDao.clearAll()
            if (mergedTextNotes.isNotEmpty()) {
                textNoteDao.insertAllTextNotes(mergedTextNotes.map { it.copy(id = 0) })
            }

            val localAudioNotes = audioNoteDao.getAllNotes()
            val mergedAudioNotes = if (useRemoteNotesAsSourceOfTruth) {
                mergePolicy.mergeAudioNotes(localAudioNotes, remoteAudioNotes)
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
            val finalLocalSessionIds = finalLocalSessions.map(mapper::sessionDocumentId).toSet() + remoteSessionsImport.unresolvedRemoteIds
            val finalLocalTextNoteIds = finalLocalTextNotes.map(mapper::textNoteDocumentId).toSet() + remoteTextNotesImport.unresolvedRemoteIds
            val finalLocalAudioNoteIds = finalLocalAudioNotes.map(mapper::audioNoteDocumentId).toSet() + remoteAudioNotesImport.unresolvedRemoteIds
            val uploadedStoragePaths = finalLocalTabs
                .filter { it.isUserTab }
                .associate { tab ->
                    tab.id to fileStore.prepareCloudTabFilePayload(
                        userId = user.uid,
                        tab = tab,
                        existingStoragePath = remoteStoragePathsByTabId[tab.id]
                    )
                }
            val uploadedAudioStoragePaths = finalLocalAudioNotes.associate { audioNote ->
                val documentId = mapper.audioNoteDocumentId(audioNote)
                documentId to fileStore.prepareCloudAudioNoteFilePayload(
                    userId = user.uid,
                    documentId = documentId,
                    audioNote = audioNote,
                    existingStoragePath = remoteAudioStoragePathsById[documentId]
                )
            }

            var batch = firestore.batch()
            var batchOperationCount = 0

            suspend fun commitBatchIfNeeded(force: Boolean = false) {
                if (batchOperationCount == 0) return
                if (!force && batchOperationCount < MaxFirestoreBatchOps) return
                batch.commit().await()
                batch = firestore.batch()
                batchOperationCount = 0
            }

            suspend fun queueSet(
                document: com.google.firebase.firestore.DocumentReference,
                data: Map<String, Any?>,
                merge: Boolean = true
            ) {
                if (merge) {
                    batch.set(document, data, SetOptions.merge())
                } else {
                    batch.set(document, data)
                }
                batchOperationCount += 1
                commitBatchIfNeeded()
            }

            suspend fun queueDelete(document: com.google.firebase.firestore.DocumentReference) {
                batch.delete(document)
                batchOperationCount += 1
                commitBatchIfNeeded()
            }

            suspend fun queueDeleteStaleDocuments(
                parent: com.google.firebase.firestore.CollectionReference,
                remoteIds: Set<String>,
                localIds: Set<String>
            ) {
                (remoteIds - localIds).forEach { staleId ->
                    queueDelete(parent.document(staleId))
                }
            }

            val settingsRef = userRef.collection("settings").document("app")
            queueSet(
                settingsRef,
                mapper.settingsToFirestoreMap(finalLocalSettings) + mapOf(
                    "sessionBackups" to FieldValue.delete(),
                    "textNoteBackups" to FieldValue.delete(),
                    "audioNoteBackups" to FieldValue.delete()
                )
            )

            finalLocalTabs.forEach { tab ->
                val tabRef = userRef.collection("tabs").document(tab.id)
                queueSet(
                    tabRef,
                    mapper.tabToFirestoreMap(tab, uploadedStoragePaths[tab.id])
                )
            }
            fileStore.deleteStaleUserTabFiles(user.uid, remoteUserTabIds, finalLocalUserTabIds)
            queueDeleteStaleDocuments(
                parent = userRef.collection("tabs"),
                remoteIds = remoteTabIds,
                localIds = finalLocalTabIds
            )

            finalLocalSessions.forEach { session ->
                val sessionRef = userRef.collection("sessions").document(mapper.sessionDocumentId(session))
                queueSet(sessionRef, mapper.sessionToFirestoreMap(session))
            }
            queueDeleteStaleDocuments(
                parent = userRef.collection("sessions"),
                remoteIds = remoteSessionsSnapshot.documents.map { it.id }.toSet(),
                localIds = finalLocalSessionIds
            )

            finalLocalGoals.forEach { goal ->
                val goalRef = userRef.collection("goals").document(goal.syncId)
                queueSet(goalRef, mapper.goalToFirestoreMap(goal))
            }
            queueDeleteStaleDocuments(
                parent = userRef.collection("goals"),
                remoteIds = remoteGoalsSnapshot.documents.map { it.id }.toSet(),
                localIds = finalLocalGoals.map { it.syncId }.toSet()
            )

            finalLocalProgress.forEach { progress ->
                val progressRef = userRef.collection("progress").document(progress.tabId)
                queueSet(progressRef, mapper.progressToFirestoreMap(progress))
            }
            queueDeleteStaleDocuments(
                parent = userRef.collection("progress"),
                remoteIds = remoteProgressSnapshot.documents.map { it.id }.toSet(),
                localIds = finalLocalProgress.map { it.tabId }.toSet()
            )

            finalLocalTextNotes.forEach { textNote ->
                val documentId = mapper.textNoteDocumentId(textNote)
                val textNoteRef = userRef.collection("text_notes").document(documentId)
                queueSet(textNoteRef, mapper.textNoteToFirestoreMap(textNote))
            }
            queueDeleteStaleDocuments(
                parent = userRef.collection("text_notes"),
                remoteIds = remoteTextNotesSnapshot.documents.map { it.id }.toSet(),
                localIds = finalLocalTextNoteIds
            )

            finalLocalAudioNotes.forEach { audioNote ->
                val documentId = mapper.audioNoteDocumentId(audioNote)
                val audioNoteRef = userRef.collection("audio_notes").document(documentId)
                queueSet(
                    audioNoteRef,
                    mapper.audioNoteToFirestoreMap(audioNote, uploadedAudioStoragePaths[documentId])
                )
            }
            fileStore.deleteStaleAudioNoteFiles(user.uid, remoteAudioNoteIds, finalLocalAudioNoteIds, remoteAudioStoragePathsById)
            queueDeleteStaleDocuments(
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

            commitBatchIfNeeded(force = true)

            val syncTimestamp = System.currentTimeMillis()
            if (preferRemoteState) {
                appSettingsRepository.clearAllPendingDeletedUserTabIds()
            } else {
                appSettingsRepository.clearPendingDeletedUserTabIds(pendingDeletedUserTabIds)
            }
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
            fileStore.deleteAllUserTabFiles(user.uid)
            fileStore.deleteAllAudioNoteFiles(user.uid)
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
        fileStore.deleteLocalAudioNoteFiles(audioNoteDao.getAllNotes())
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

}
