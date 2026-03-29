package com.example.thetest1.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.example.thetest1.data.settings.AppSettingsRepository
import com.example.thetest1.data.settings.AppSettingsSnapshot
import com.example.thetest1.domain.model.Difficulty
import com.example.thetest1.domain.model.Goal
import com.example.thetest1.domain.model.GoalType
import com.example.thetest1.domain.model.PracticedTab
import com.example.thetest1.domain.model.Session
import com.example.thetest1.domain.model.TabItem
import com.example.thetest1.domain.model.TabPlaybackProgress
import com.example.thetest1.domain.repository.GoalRepository
import com.example.thetest1.domain.repository.SessionRepository
import com.example.thetest1.domain.repository.SyncRepository
import com.example.thetest1.domain.repository.TabPlaybackProgressRepository
import com.example.thetest1.domain.repository.TabRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.io.File
import java.security.MessageDigest

class FirestoreSyncRepositoryImpl(
    private val context: Context,
    private val tabRepository: TabRepository,
    private val sessionRepository: SessionRepository,
    private val goalRepository: GoalRepository,
    private val progressRepository: TabPlaybackProgressRepository,
    private val appSettingsRepository: AppSettingsRepository
) : SyncRepository {
    private companion object {
        const val MaxInlineUserTabBytes = 512 * 1024L
    }

    private data class CloudTabFilePayload(
        val storagePath: String?,
        val fileBase64: String?
    )

    private data class RemoteTabsImportResult(
        val tabs: List<TabItem>,
        val unresolvedRemoteIds: Set<String>
    )

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
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
        val user = auth.currentUser ?: return Result.failure(Exception("Користувач не авторизований"))

        syncingState.value = true
        return runCatching {
            val userRef = firestore.collection("users").document(user.uid)
            val previousOwnerUid = appSettingsRepository.getSyncOwnerUid()
            val isAccountSwitch = !previousOwnerUid.isNullOrBlank() && previousOwnerUid != user.uid

            if (isAccountSwitch) {
                clearLocalCloudScopedData()
                ensureBuiltInTabsSeeded()
            }

            val remoteSettings = userRef.collection("settings").document("app").get().await().toSettings()
            val remoteTabsSnapshot = userRef.collection("tabs").get().await()
            val remoteSessionsSnapshot = userRef.collection("sessions").get().await()
            val remoteGoalsSnapshot = userRef.collection("goals").get().await()
            val remoteProgressSnapshot = userRef.collection("progress").get().await()
            val remoteStoragePathsByTabId = remoteTabsSnapshot.documents.associate { it.id to it.getString("storagePath") }
            val remoteTabsImport = importRemoteTabs(remoteTabsSnapshot.documents)
            val remoteTabs = remoteTabsImport.tabs
            val remoteSessions = remoteSessionsSnapshot.documents.mapNotNull { it.toSession() }
            val remoteGoals = remoteGoalsSnapshot.documents.mapNotNull { it.toGoal() }
            val remoteProgress = remoteProgressSnapshot.documents.mapNotNull { it.toProgress() }

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

            val finalLocalTabs = tabRepository.getAllTabsSync()
            val finalLocalSessions = sessionRepository.getAllSessionsSync()
            val finalLocalGoals = goalRepository.getGoalsSync()
            val finalLocalProgress = progressRepository.observeAll().first()
            val finalLocalSettings = appSettingsRepository.getSettings()
            val remoteTabIds = remoteTabsSnapshot.documents.map { it.id }.toSet()
            val remoteUserTabIds = remoteTabsSnapshot.documents
                .filter { it.getBoolean("isUserTab") == true }
                .map { it.getString("id") ?: it.id }
                .toSet()
            val finalLocalTabIds = finalLocalTabs.map { it.id }.toSet() + remoteTabsImport.unresolvedRemoteIds
            val finalLocalUserTabIds = finalLocalTabs
                .filter { it.isUserTab }
                .map { it.id }
                .toSet() + remoteTabsImport.unresolvedRemoteIds
            val uploadedStoragePaths = finalLocalTabs
                .filter { it.isUserTab }
                .associate { tab ->
                    tab.id to prepareCloudTabFilePayload(
                        userId = user.uid,
                        tab = tab,
                        existingStoragePath = remoteStoragePathsByTabId[tab.id]
                    )
                }

            val batch = firestore.batch()

            val settingsRef = userRef.collection("settings").document("app")
            batch.set(settingsRef, finalLocalSettings.toFirestoreMap(), SetOptions.merge())

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
                localIds = finalLocalSessions.map(::sessionDocumentId).toSet()
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

            batch.commit().await()

            val syncTimestamp = System.currentTimeMillis()
            appSettingsRepository.setSyncOwnerUid(user.uid)
            appSettingsRepository.setLastCloudSyncAt(syncTimestamp)
        }
            .onFailure {
                // no-op, just let Result carry the error
            }
            .also {
                syncingState.value = false
            }
    }

    override suspend fun clearRemoteData(): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Користувач не авторизований"))
        syncingState.value = true
        return runCatching {
            val userRef = firestore.collection("users").document(user.uid)
            val collections = listOf("tabs", "sessions", "goals", "progress")
            collections.forEach { name ->
                val snapshot = userRef.collection(name).get().await()
                if (snapshot.isEmpty) return@forEach
                val batch = firestore.batch()
                snapshot.documents.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
            userRef.collection("settings").document("app").delete().await()
            deleteAllUserTabFiles(user.uid)
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
        tabRepository.clearAllTabs()
        sessionRepository.clearHistory()
        goalRepository.clearGoals()
        progressRepository.clearAll()
        appSettingsRepository.resetSettingsToDefaults()
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
            "folder" to folder,
            "openCount" to openCount,
            "lastOpenedAt" to lastOpenedAt,
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
            val startTime = getDate("startTime") ?: return null
            val endTime = getDate("endTime") ?: return null
            val duration = getLong("duration") ?: 0L
            val practicedTabsRaw = get("practicedTabs") as? List<*> ?: emptyList<Any>()
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
                folder = getString("folder") ?: "Без папки",
                openCount = getLong("openCount")?.toInt() ?: 0,
                lastOpenedAt = getLong("lastOpenedAt") ?: 0L,
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

        throw IllegalStateException("Не вдалося синхронізувати файл таба \"${tab.name}\"")
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

    private suspend fun deleteStaleUserTabFiles(
        userId: String,
        remoteIds: Set<String>,
        localIds: Set<String>
    ) {
        (remoteIds - localIds).forEach { staleId ->
            deleteUserTabFile(userId, staleId)
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

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, fallback: T): T {
        return value?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: fallback
    }
}
