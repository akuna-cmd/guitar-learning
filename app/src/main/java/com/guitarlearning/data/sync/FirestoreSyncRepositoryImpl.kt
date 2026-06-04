package com.guitarlearning.data.sync

import android.content.Context
import android.util.Log
import com.guitarlearning.R
import com.guitarlearning.data.local.dao.AudioNoteDao
import com.guitarlearning.data.local.dao.TextNoteDao
import com.guitarlearning.data.local.entity.toDomain
import com.guitarlearning.data.local.entity.toEntity
import com.guitarlearning.domain.model.PracticedTab
import com.guitarlearning.domain.model.Session
import com.guitarlearning.domain.model.TabItem
import com.guitarlearning.domain.repository.AppSettingsRepository
import com.guitarlearning.domain.repository.GoalRepository
import com.guitarlearning.domain.repository.SessionRepository
import com.guitarlearning.domain.repository.SyncRepository
import com.guitarlearning.domain.repository.TabPlaybackProgressRepository
import com.guitarlearning.domain.repository.TabRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
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
    private val remoteSyncWriter = RemoteSyncWriter(
        firestore = firestore,
        mapper = mapper,
        fileStore = fileStore,
        logTag = LogTag,
        maxBatchOps = MaxFirestoreBatchOps
    )

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
        val user = auth.currentUser
            ?: return Result.failure(Exception(context.getString(R.string.sync_error_not_authorized)))

        syncingState.value = true
        return runCatching {
            Log.d(LogTag, "syncData:start uid=${user.uid} email=${user.email.orEmpty()}")
            val userRef = firestore.collection("users").document(user.uid)
            val syncContext = buildSyncContext(user.uid)
            logSyncContext(user.uid, syncContext)
            clearLocalStateForAccountSwitch(user.uid, syncContext)

            val remoteState = fetchRemoteState(userRef, syncContext)
            logRemoteState(user.uid, remoteState)

            val localState = applyRemoteState(syncContext, remoteState)
            logLocalMergeState(user.uid, localState)

            val uploadState = collectUploadState(user.uid, remoteState)
            writeRemoteState(userRef, user.uid, remoteState, uploadState)
            finalizeSync(user.uid, syncContext)
            Unit
        }.onFailure { error ->
            Log.e(LogTag, "syncData:failure uid=${user.uid}", error)
        }.also {
            syncingState.value = false
        }
    }

    override suspend fun clearRemoteData(): Result<Unit> {
        val user = auth.currentUser
            ?: return Result.failure(Exception(context.getString(R.string.sync_error_not_authorized)))

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

    private suspend fun buildSyncContext(userUid: String): SyncContext {
        val previousOwnerUid = appSettingsRepository.getSyncOwnerUid()
        val storedPendingDeletedUserTabIds = appSettingsRepository.getPendingDeletedUserTabIds()
        val isAccountSwitch = !previousOwnerUid.isNullOrBlank() && previousOwnerUid != userUid
        val preferRemoteState = previousOwnerUid.isNullOrBlank() || isAccountSwitch
        val pendingDeletedUserTabIds = if (preferRemoteState) emptySet() else storedPendingDeletedUserTabIds
        return SyncContext(
            previousOwnerUid = previousOwnerUid,
            preferRemoteState = preferRemoteState,
            isAccountSwitch = isAccountSwitch,
            pendingDeletedUserTabIds = pendingDeletedUserTabIds
        )
    }

    private fun logSyncContext(userUid: String, syncContext: SyncContext) {
        Log.d(
            LogTag,
            "syncData:context uid=$userUid previousOwnerUid=${syncContext.previousOwnerUid.orEmpty()} " +
                "pendingDeletedTabs=${syncContext.pendingDeletedUserTabIds.size} isAccountSwitch=${syncContext.isAccountSwitch}"
        )
    }

    private suspend fun clearLocalStateForAccountSwitch(userUid: String, syncContext: SyncContext) {
        if (!syncContext.isAccountSwitch) return
        Log.w(LogTag, "syncData:accountSwitch clearing local cloud-scoped data for uid=$userUid")
        clearLocalCloudScopedData()
        ensureBuiltInTabsSeeded()
    }

    private suspend fun fetchRemoteState(
        userRef: com.google.firebase.firestore.DocumentReference,
        syncContext: SyncContext
    ): RemoteSyncState {
        val settingsDocument = userRef.collection("settings").document("app").get().await()
        val tabsSnapshot = userRef.collection("tabs").get().await()
        val sessionsSnapshot = userRef.collection("sessions").get().await()
        val goalsSnapshot = userRef.collection("goals").get().await()
        val progressSnapshot = userRef.collection("progress").get().await()
        val textNotesSnapshot = userRef.collection("text_notes").get().await()
        val audioNotesSnapshot = userRef.collection("audio_notes").get().await()

        val remoteStoragePathsByTabId = tabsSnapshot.documents.associate { it.id to it.getString("storagePath") }
        val remoteAudioStoragePathsById = audioNotesSnapshot.documents.associate { it.id to it.getString("storagePath") }
        val filteredRemoteTabDocuments = tabsSnapshot.documents.filterNot { document ->
            val remoteId = document.getString("id") ?: document.id
            document.getBoolean("isUserTab") == true && remoteId in syncContext.pendingDeletedUserTabIds
        }

        val tabsImport = mapper.importRemoteTabs(filteredRemoteTabDocuments)
        val sessionBackups = mapper.toSessionBackups(settingsDocument)
        val textNoteBackups = mapper.toTextNoteBackups(settingsDocument)
        val audioNoteBackups = mapper.toAudioNoteBackups(settingsDocument)

        val usedSessionFallback = sessionsSnapshot.isEmpty
        val sessionsImport = if (usedSessionFallback) {
            RemoteImportResult(items = sessionBackups, unresolvedRemoteIds = emptySet())
        } else {
            mapper.importRemoteSessions(sessionsSnapshot.documents)
        }
        val sessions = resolveRemoteItemsWithBackupFallback(
            imported = sessionsImport,
            backups = sessionBackups,
            fallbackLogMessage = "syncData:using settings session backups because collection docs are unreadable"
        )

        val usedTextNoteFallback = textNotesSnapshot.isEmpty
        val textNotesImport = if (usedTextNoteFallback) {
            RemoteImportResult(items = textNoteBackups, unresolvedRemoteIds = emptySet())
        } else {
            mapper.importRemoteTextNotes(textNotesSnapshot.documents)
        }
        val textNotes = resolveRemoteItemsWithBackupFallback(
            imported = textNotesImport,
            backups = textNoteBackups,
            fallbackLogMessage = "syncData:using settings text note backups because collection docs are unreadable"
        )

        val usedAudioNoteFallback = audioNotesSnapshot.isEmpty
        val audioNotesImport = if (usedAudioNoteFallback) {
            RemoteImportResult(items = audioNoteBackups, unresolvedRemoteIds = emptySet())
        } else {
            mapper.importRemoteAudioNotes(audioNotesSnapshot.documents)
        }
        val audioNotes = resolveRemoteItemsWithBackupFallback(
            imported = audioNotesImport,
            backups = audioNoteBackups,
            fallbackLogMessage = "syncData:using settings audio note backups because collection docs are unreadable"
        )

        return RemoteSyncState(
            settings = mapper.toSettings(settingsDocument),
            settingsDocument = DocumentReferenceSnapshotBundle(settingsDocument),
            tabsSnapshot = tabsSnapshot,
            sessionsSnapshot = sessionsSnapshot,
            goalsSnapshot = goalsSnapshot,
            progressSnapshot = progressSnapshot,
            textNotesSnapshot = textNotesSnapshot,
            audioNotesSnapshot = audioNotesSnapshot,
            remoteStoragePathsByTabId = remoteStoragePathsByTabId,
            remoteAudioStoragePathsById = remoteAudioStoragePathsById,
            tabsImport = tabsImport,
            sessionsImport = sessionsImport,
            textNotesImport = textNotesImport,
            audioNotesImport = audioNotesImport,
            tabs = tabsImport.tabs,
            sessions = sessions,
            goals = goalsSnapshot.documents.mapNotNull(mapper::toGoal),
            progress = progressSnapshot.documents.mapNotNull(mapper::toProgress),
            textNotes = textNotes,
            audioNotes = audioNotes,
            usedSessionFallback = usedSessionFallback,
            usedTextNoteFallback = usedTextNoteFallback,
            usedAudioNoteFallback = usedAudioNoteFallback,
            remoteTabIds = tabsSnapshot.documents.map { it.id }.toSet(),
            remoteUserTabIds = tabsSnapshot.documents
                .filter { it.getBoolean("isUserTab") == true }
                .map { it.getString("id") ?: it.id }
                .toSet(),
            remoteAudioNoteIds = audioNotesSnapshot.documents.map { it.id }.toSet()
        )
    }

    private fun logRemoteState(userUid: String, remoteState: RemoteSyncState) {
        Log.d(
            LogTag,
            "syncData:remote uid=$userUid tabs=${remoteState.tabs.size}/${remoteState.tabsSnapshot.size()} unresolvedTabs=${remoteState.tabsImport.unresolvedRemoteIds.size} " +
                "sessions=${remoteState.sessions.size}/${remoteState.sessionsSnapshot.size()} unresolvedSessions=${remoteState.sessionsImport.unresolvedRemoteIds.size} sessionFallback=${remoteState.usedSessionFallback} " +
                "goals=${remoteState.goals.size}/${remoteState.goalsSnapshot.size()} progress=${remoteState.progress.size}/${remoteState.progressSnapshot.size()} " +
                "textNotes=${remoteState.textNotes.size}/${remoteState.textNotesSnapshot.size()} unresolvedTextNotes=${remoteState.textNotesImport.unresolvedRemoteIds.size} textFallback=${remoteState.usedTextNoteFallback} " +
                "audioNotes=${remoteState.audioNotes.size}/${remoteState.audioNotesSnapshot.size()} unresolvedAudioNotes=${remoteState.audioNotesImport.unresolvedRemoteIds.size} audioFallback=${remoteState.usedAudioNoteFallback}"
        )
    }

    private suspend fun applyRemoteState(
        syncContext: SyncContext,
        remoteState: RemoteSyncState
    ): LocalSyncState {
        val localSettings = appSettingsRepository.getSettings()
        val mergedSettings = mergePolicy.mergeSettings(
            local = localSettings,
            remote = remoteState.settings,
            preferRemote = syncContext.preferRemoteState
        )
        appSettingsRepository.replaceSettings(mergedSettings)

        val localTabs = tabRepository.getAllTabsSync()
        val mergedTabsResult = mergePolicy.mergeTabCollections(localTabs, remoteState.tabs)
        if (mergedTabsResult.tabsToDelete.isNotEmpty()) {
            tabRepository.deleteTabs(mergedTabsResult.tabsToDelete)
        }
        if (mergedTabsResult.tabs.isNotEmpty()) {
            tabRepository.upsertTabs(mergedTabsResult.tabs)
        }

        val availableTabIds = tabRepository.getAllTabsSync().mapTo(mutableSetOf()) { it.id }
        val sanitizedSessions = remoteState.sessions.sanitizeForAvailableTabs(availableTabIds)
        val droppedSessions = remoteState.sessions.size - sanitizedSessions.size
        val droppedPracticedTabs = remoteState.sessions.sumOf { it.practicedTabs.size } -
            sanitizedSessions.sumOf { it.practicedTabs.size }
        if (droppedSessions > 0 || droppedPracticedTabs > 0) {
            Log.w(
                LogTag,
                "syncData:sanitizedSessions droppedSessions=$droppedSessions droppedPracticedTabs=$droppedPracticedTabs availableTabs=${availableTabIds.size}"
            )
        }
        sessionRepository.importHistory(sanitizedSessions)

        val localGoals = goalRepository.getGoalsSync()
        val mergedGoals = mergePolicy.mergeGoals(localGoals, remoteState.goals)
        goalRepository.clearGoals()
        if (mergedGoals.isNotEmpty()) {
            goalRepository.upsertGoals(mergedGoals)
        }

        val localProgress = progressRepository.observeAll().first()
        progressRepository.replaceAll(mergePolicy.mergeProgress(localProgress, remoteState.progress))

        val localTextNotes = textNoteDao.getAllTextNotes().map { it.toDomain() }
        val localAudioNotes = audioNoteDao.getAllNotes().map { it.toDomain() }
        val useRemoteNotesAsSourceOfTruth = syncContext.preferRemoteState
        val mergedTextNotes = if (useRemoteNotesAsSourceOfTruth) {
            mergePolicy.mergeTextNotes(localTextNotes, remoteState.textNotes)
        } else {
            localTextNotes.sortedByDescending { it.createdAt.time }
        }
        val mergedAudioNotes = if (useRemoteNotesAsSourceOfTruth) {
            mergePolicy.mergeAudioNotes(localAudioNotes, remoteState.audioNotes)
        } else {
            localAudioNotes.sortedByDescending { it.createdAt.time }
        }

        textNoteDao.clearAll()
        if (mergedTextNotes.isNotEmpty()) {
            textNoteDao.insertAllTextNotes(mergedTextNotes.map { it.copy(id = 0).toEntity() })
        }
        audioNoteDao.clearAll()
        if (mergedAudioNotes.isNotEmpty()) {
            audioNoteDao.insertAll(mergedAudioNotes.map { it.copy(id = 0).toEntity() })
        }

        return LocalSyncState(
            localSettings = localSettings,
            localTabs = localTabs,
            localGoals = localGoals,
            localProgress = localProgress,
            localTextNotes = localTextNotes,
            localAudioNotes = localAudioNotes,
            mergedTextNotes = mergedTextNotes,
            mergedAudioNotes = mergedAudioNotes,
            useRemoteNotesAsSourceOfTruth = useRemoteNotesAsSourceOfTruth
        )
    }

    private suspend fun logLocalMergeState(userUid: String, localState: LocalSyncState) {
        Log.d(
            LogTag,
            "syncData:merge uid=$userUid localTabs=${localState.localTabs.size} localSessionsBefore=${sessionRepository.getAllSessionsSync().size} " +
                "localGoals=${localState.localGoals.size} localProgress=${localState.localProgress.size} localTextNotes=${localState.localTextNotes.size} localAudioNotes=${localState.localAudioNotes.size} " +
                "mergedTextNotes=${localState.mergedTextNotes.size} mergedAudioNotes=${localState.mergedAudioNotes.size} " +
                "useRemoteNotesAsSourceOfTruth=${localState.useRemoteNotesAsSourceOfTruth}"
        )
    }

    private suspend fun collectUploadState(
        userUid: String,
        remoteState: RemoteSyncState
    ): UploadSyncState {
        val finalTabs = tabRepository.getAllTabsSync()
        val finalSessions = sessionRepository.getAllSessionsSync()
        val finalGoals = goalRepository.getGoalsSync()
        val finalProgress = progressRepository.observeAll().first()
        val finalTextNotes = textNoteDao.getAllTextNotes().map { it.toDomain() }
        val finalAudioNotes = audioNoteDao.getAllNotes().map { it.toDomain() }
        val finalSettings = appSettingsRepository.getSettings()

        val finalTabIds = finalTabs.map { it.id }.toSet() + remoteState.tabsImport.unresolvedRemoteIds
        val finalUserTabIds = finalTabs.filter { it.isUserTab }.map { it.id }.toSet() + remoteState.tabsImport.unresolvedRemoteIds
        val finalSessionIds = finalSessions.map(mapper::sessionDocumentId).toSet() + remoteState.sessionsImport.unresolvedRemoteIds
        val finalTextNoteIds = finalTextNotes.map(mapper::textNoteDocumentId).toSet() + remoteState.textNotesImport.unresolvedRemoteIds
        val finalAudioNoteIds = finalAudioNotes.map(mapper::audioNoteDocumentId).toSet() + remoteState.audioNotesImport.unresolvedRemoteIds

        val uploadedStoragePaths = finalTabs
            .filter { it.isUserTab }
            .associate { tab ->
                tab.id to fileStore.prepareCloudTabFilePayload(
                    userId = userUid,
                    tab = tab,
                    existingStoragePath = remoteState.remoteStoragePathsByTabId[tab.id]
                )
            }

        val uploadedAudioStoragePaths = finalAudioNotes.associate { audioNote ->
            val documentId = mapper.audioNoteDocumentId(audioNote)
            documentId to fileStore.prepareCloudAudioNoteFilePayload(
                userId = userUid,
                documentId = documentId,
                audioNote = audioNote,
                existingStoragePath = remoteState.remoteAudioStoragePathsById[documentId]
            )
        }

        return UploadSyncState(
            finalSettings = finalSettings,
            finalTabs = finalTabs,
            finalSessions = finalSessions,
            finalGoals = finalGoals,
            finalProgress = finalProgress,
            finalTextNotes = finalTextNotes,
            finalAudioNotes = finalAudioNotes,
            finalTabIds = finalTabIds,
            finalUserTabIds = finalUserTabIds,
            finalSessionIds = finalSessionIds,
            finalTextNoteIds = finalTextNoteIds,
            finalAudioNoteIds = finalAudioNoteIds,
            uploadedStoragePaths = uploadedStoragePaths,
            uploadedAudioStoragePaths = uploadedAudioStoragePaths
        )
    }

    private suspend fun writeRemoteState(
        userRef: com.google.firebase.firestore.DocumentReference,
        userUid: String,
        remoteState: RemoteSyncState,
        uploadState: UploadSyncState
    ) {
        remoteSyncWriter.writeRemoteState(userRef, userUid, remoteState, uploadState)
    }

    private suspend fun finalizeSync(userUid: String, syncContext: SyncContext) {
        val syncTimestamp = System.currentTimeMillis()
        if (syncContext.preferRemoteState) {
            appSettingsRepository.clearAllPendingDeletedUserTabIds()
        } else {
            appSettingsRepository.clearPendingDeletedUserTabIds(syncContext.pendingDeletedUserTabIds)
        }
        appSettingsRepository.setSyncOwnerUid(userUid)
        appSettingsRepository.setLastCloudSyncAt(syncTimestamp)
        Log.d(LogTag, "syncData:success uid=$userUid syncedAt=$syncTimestamp")
    }

    private suspend fun clearLocalCloudScopedData() {
        Log.w(LogTag, "clearLocalCloudScopedData:start")
        fileStore.deleteLocalAudioNoteFiles(audioNoteDao.getAllNotes().map { it.toDomain() })
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

    private fun <T> resolveRemoteItemsWithBackupFallback(
        imported: RemoteImportResult<T>,
        backups: List<T>,
        fallbackLogMessage: String
    ): List<T> {
        return if (imported.items.isEmpty() &&
            imported.unresolvedRemoteIds.isNotEmpty() &&
            backups.isNotEmpty()
        ) {
            Log.w(LogTag, fallbackLogMessage)
            backups
        } else {
            imported.items
        }
    }
}

private fun List<Session>.sanitizeForAvailableTabs(availableTabIds: Set<String>): List<Session> {
    return mapNotNull { session ->
        val validPracticedTabs = session.practicedTabs.filter { practicedTab ->
            practicedTab.tabId in availableTabIds
        }
        when {
            validPracticedTabs.isEmpty() && session.practicedTabs.isNotEmpty() -> null
            validPracticedTabs.size == session.practicedTabs.size -> session
            else -> session.copy(practicedTabs = validPracticedTabs)
        }
    }
}
