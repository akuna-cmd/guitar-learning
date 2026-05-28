package com.guitarlearning.data.repository

import android.util.Log
import com.guitarlearning.core.settings.AiProvider
import com.guitarlearning.data.settings.AppSettingsSnapshot
import com.guitarlearning.domain.model.AudioNote
import com.guitarlearning.domain.model.DEFAULT_TAB_FOLDER_KEY
import com.guitarlearning.domain.model.Difficulty
import com.guitarlearning.domain.model.Goal
import com.guitarlearning.domain.model.GoalType
import com.guitarlearning.domain.model.PracticedTab
import com.guitarlearning.domain.model.Session
import com.guitarlearning.domain.model.TabItem
import com.guitarlearning.domain.model.TabPlaybackProgress
import com.guitarlearning.domain.model.TextNote
import com.guitarlearning.domain.model.normalizeTabFolder
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.io.File
import java.security.MessageDigest
import java.util.Date

internal data class CloudTabFilePayload(
    val storagePath: String?,
    val fileBase64: String?
)

internal data class CloudAudioNoteFilePayload(
    val storagePath: String?,
    val fileBase64: String?
)

internal data class RemoteTabsImportResult(
    val tabs: List<TabItem>,
    val unresolvedRemoteIds: Set<String>
)

internal data class RemoteImportResult<T>(
    val items: List<T>,
    val unresolvedRemoteIds: Set<String>
)

internal class FirestoreSyncMapper(
    private val logTag: String,
    private val restoreUserTabFile: suspend (
        tabId: String,
        tabName: String,
        originalPath: String?,
        storagePath: String?,
        fileBase64: String?
    ) -> String?,
    private val restoreAudioNoteFile: suspend (
        documentId: String,
        fileName: String?,
        storagePath: String?,
        fileBase64: String?
    ) -> String?
) {
    fun settingsToFirestoreMap(settings: AppSettingsSnapshot): Map<String, Any> {
        return mapOf(
            "themeMode" to settings.themeMode.name,
            "appLanguage" to settings.appLanguage.name,
            "aiProvider" to settings.aiProvider.name,
            "localAiServerUrl" to settings.localAiServerUrl,
            "normalSpeed" to settings.normalSpeed,
            "practiceSpeed" to settings.practiceSpeed,
            "normalTabScale" to settings.normalTabScale,
            "practiceTabScale" to settings.practiceTabScale,
            "tabDisplayMode" to settings.tabDisplayMode.name,
            "fretboardDisplayMode" to settings.fretboardDisplayMode.name,
            "updatedAt" to settings.updatedAt
        )
    }

    fun tabToFirestoreMap(tab: TabItem, filePayload: CloudTabFilePayload?): Map<String, Any?> {
        return mapOf(
            "id" to tab.id,
            "name" to tab.name,
            "description" to tab.description,
            "difficulty" to tab.difficulty.name,
            "lessonNumber" to tab.lessonNumber,
            "isCompleted" to tab.isCompleted,
            "isUserTab" to tab.isUserTab,
            "filePath" to tab.filePath,
            "storagePath" to filePayload?.storagePath,
            "fileBase64" to filePayload?.fileBase64,
            "asciiTabs" to tab.asciiTabs,
            "tagsCsv" to tab.tagsCsv,
            "folder" to normalizeTabFolder(tab.folder),
            "openCount" to tab.openCount,
            "lastOpenedAt" to tab.lastOpenedAt,
            "createdAt" to tab.createdAt,
            "updatedAt" to tab.updatedAt,
            "offlineReady" to tab.offlineReady
        )
    }

    fun sessionToFirestoreMap(session: Session): Map<String, Any> {
        return mapOf(
            "startTime" to session.startTime.time,
            "endTime" to session.endTime.time,
            "duration" to session.duration,
            "practicedTabs" to session.practicedTabs.map(::practicedTabToFirestoreMap)
        )
    }

    fun textNoteToFirestoreMap(textNote: TextNote): Map<String, Any> {
        return mapOf(
            "lessonId" to textNote.lessonId,
            "content" to textNote.content,
            "createdAt" to textNote.createdAt.time,
            "isFavorite" to textNote.isFavorite
        )
    }

    fun audioNoteToFirestoreMap(audioNote: AudioNote, filePayload: CloudAudioNoteFilePayload?): Map<String, Any?> {
        return mapOf(
            "lessonId" to audioNote.lessonId,
            "fileName" to File(audioNote.filePath).name,
            "storagePath" to filePayload?.storagePath,
            "fileBase64" to filePayload?.fileBase64,
            "createdAt" to audioNote.createdAt.time,
            "isFavorite" to audioNote.isFavorite
        )
    }

    fun audioNoteToFirestoreBackupMap(
        audioNote: AudioNote,
        documentId: String,
        filePayload: CloudAudioNoteFilePayload?
    ): Map<String, Any?> {
        return audioNoteToFirestoreMap(audioNote, filePayload).toMutableMap().apply {
            put("documentId", documentId)
        }
    }

    fun goalToFirestoreMap(goal: Goal): Map<String, Any> {
        return mapOf(
            "id" to goal.id,
            "syncId" to goal.syncId,
            "type" to goal.type.name,
            "description" to goal.description,
            "target" to goal.target,
            "progress" to goal.progress,
            "deadline" to goal.deadline,
            "updatedAt" to goal.updatedAt,
            "isCompleted" to goal.isCompleted,
            "isOverdue" to goal.isOverdue
        )
    }

    fun progressToFirestoreMap(progress: TabPlaybackProgress): Map<String, Any> {
        return mapOf(
            "tabId" to progress.tabId,
            "tabName" to progress.tabName,
            "lastTick" to progress.lastTick,
            "lastBarIndex" to progress.lastBarIndex,
            "totalBars" to progress.totalBars,
            "updatedAt" to progress.updatedAt
        )
    }

    fun toSettings(document: DocumentSnapshot): AppSettingsSnapshot? {
        if (!document.exists()) return null
        return AppSettingsSnapshot(
            themeMode = enumValueOrDefault(document.getString("themeMode"), AppSettingsSnapshot().themeMode),
            appLanguage = enumValueOrDefault(document.getString("appLanguage"), AppSettingsSnapshot().appLanguage),
            aiProvider = enumValueOrDefault(document.getString("aiProvider"), AppSettingsSnapshot().aiProvider),
            localAiServerUrl = document.getString("localAiServerUrl").orEmpty(),
            normalSpeed = document.getDouble("normalSpeed")?.toFloat() ?: 1.0f,
            practiceSpeed = document.getDouble("practiceSpeed")?.toFloat() ?: 0.25f,
            normalTabScale = document.getDouble("normalTabScale")?.toFloat() ?: 1.0f,
            practiceTabScale = document.getDouble("practiceTabScale")?.toFloat() ?: 1.0f,
            tabDisplayMode = enumValueOrDefault(document.getString("tabDisplayMode"), AppSettingsSnapshot().tabDisplayMode),
            fretboardDisplayMode = enumValueOrDefault(
                document.getString("fretboardDisplayMode"),
                AppSettingsSnapshot().fretboardDisplayMode
            ),
            updatedAt = document.getLong("updatedAt") ?: 0L
        )
    }

    fun toSession(document: DocumentSnapshot): Session? {
        return runCatching {
            val startTime = document.getDateCompat("startTime") ?: return null
            val endTime = document.getDateCompat("endTime") ?: return null
            val duration = document.getLongCompat("duration") ?: 0L
            val practicedTabsRaw = document.get("practicedTabs") as? List<*> ?: emptyList<Any>()
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
            Log.w(logTag, "toSession:unreadable docId=${document.id} data=${document.data.orEmpty()}", error)
        }.getOrNull()
    }

    fun toTextNote(document: DocumentSnapshot): TextNote? {
        return runCatching {
            val lessonId = document.getStringCompat("lessonId") ?: return null
            val createdAt = document.getDateCompat("createdAt") ?: return null
            TextNote(
                lessonId = lessonId,
                content = document.getStringCompat("content").orEmpty(),
                createdAt = createdAt,
                isFavorite = document.getBooleanCompat("isFavorite") ?: false
            )
        }.onFailure { error ->
            Log.w(logTag, "toTextNote:unreadable docId=${document.id} data=${document.data.orEmpty()}", error)
        }.getOrNull()
    }

    fun toSessionBackups(document: DocumentSnapshot): List<Session> {
        val rawItems = document.get("sessionBackups") as? List<*> ?: return emptyList()
        return rawItems.mapNotNull { (it as? Map<*, *>)?.toSessionBackup() }
    }

    fun toTextNoteBackups(document: DocumentSnapshot): List<TextNote> {
        val rawItems = document.get("textNoteBackups") as? List<*> ?: return emptyList()
        return rawItems.mapNotNull { (it as? Map<*, *>)?.toTextNoteBackup() }
    }

    suspend fun toAudioNoteBackups(document: DocumentSnapshot): List<AudioNote> {
        val rawItems = document.get("audioNoteBackups") as? List<*> ?: return emptyList()
        return rawItems.mapNotNull { (it as? Map<*, *>)?.toAudioNoteBackup() }
    }

    suspend fun toAudioNote(document: DocumentSnapshot): AudioNote? {
        return runCatching {
            val lessonId = document.getStringCompat("lessonId") ?: return null
            val createdAt = document.getDateCompat("createdAt") ?: return null
            val restoredPath = restoreAudioNoteFile(
                document.id,
                document.getStringCompat("fileName"),
                document.getStringCompat("storagePath"),
                document.getStringCompat("fileBase64")
            ) ?: return null
            AudioNote(
                lessonId = lessonId,
                filePath = restoredPath,
                createdAt = createdAt,
                isFavorite = document.getBooleanCompat("isFavorite") ?: false
            )
        }.onFailure { error ->
            Log.w(logTag, "toAudioNote:unreadable docId=${document.id} data=${document.data.orEmpty()}", error)
        }.getOrNull()
    }

    suspend fun toTabItem(document: DocumentSnapshot): TabItem? {
        return runCatching {
            val isUserTab = document.getBoolean("isUserTab") ?: false
            val originalPath = document.getString("filePath")
            val localPath = if (isUserTab) {
                restoreUserTabFile(
                    document.getString("id") ?: document.id,
                    document.getString("name") ?: "",
                    originalPath,
                    document.getString("storagePath"),
                    document.getString("fileBase64")
                )
            } else {
                originalPath
            }

            if (isUserTab && localPath.isNullOrBlank()) {
                return null
            }

            TabItem(
                id = document.getString("id") ?: document.id,
                name = document.getString("name") ?: "",
                description = document.getString("description") ?: "",
                difficulty = enumValueOrDefault(document.getString("difficulty"), Difficulty.BEGINNER),
                lessonNumber = document.getLong("lessonNumber")?.toInt() ?: 0,
                isCompleted = document.getBoolean("isCompleted") ?: false,
                isUserTab = isUserTab,
                filePath = localPath,
                asciiTabs = document.getString("asciiTabs"),
                tagsCsv = document.getString("tagsCsv") ?: "",
                folder = normalizeTabFolder(document.getString("folder") ?: DEFAULT_TAB_FOLDER_KEY),
                openCount = document.getLong("openCount")?.toInt() ?: 0,
                lastOpenedAt = document.getLong("lastOpenedAt") ?: 0L,
                createdAt = document.getLong("createdAt")
                    ?: document.getLong("updatedAt")
                    ?: document.getLong("lastOpenedAt")
                    ?: 0L,
                updatedAt = document.getLong("updatedAt") ?: 0L,
                offlineReady = document.getBoolean("offlineReady") ?: false
            )
        }.getOrNull()
    }

    fun toGoal(document: DocumentSnapshot): Goal? {
        return runCatching {
            Goal(
                id = document.getLong("id")?.toInt() ?: 0,
                syncId = document.getString("syncId") ?: document.id,
                type = enumValueOrDefault(document.getString("type"), GoalType.CUSTOM),
                description = document.getString("description") ?: "",
                target = document.getLong("target")?.toInt() ?: 0,
                progress = document.getLong("progress")?.toInt() ?: 0,
                deadline = document.getLong("deadline") ?: 0L,
                updatedAt = document.getLong("updatedAt") ?: 0L,
                isCompleted = document.getBoolean("isCompleted") ?: false,
                isOverdue = document.getBoolean("isOverdue") ?: false
            )
        }.getOrNull()
    }

    fun toProgress(document: DocumentSnapshot): TabPlaybackProgress? {
        return runCatching {
            TabPlaybackProgress(
                tabId = document.getString("tabId") ?: document.id,
                tabName = document.getString("tabName") ?: "",
                lastTick = document.getLong("lastTick") ?: 0L,
                lastBarIndex = document.getLong("lastBarIndex")?.toInt() ?: 0,
                totalBars = document.getLong("totalBars")?.toInt() ?: 0,
                updatedAt = document.getLong("updatedAt") ?: 0L
            )
        }.getOrNull()
    }

    suspend fun importRemoteTabs(documents: List<DocumentSnapshot>): RemoteTabsImportResult {
        val tabs = mutableListOf<TabItem>()
        val unresolvedRemoteIds = mutableSetOf<String>()

        documents.forEach { document ->
            val remoteId = document.getString("id") ?: document.id
            val importedTab = toTabItem(document)
            if (importedTab != null) {
                tabs += importedTab
            } else if (document.getBoolean("isUserTab") == true) {
                unresolvedRemoteIds += remoteId
            }
        }

        return RemoteTabsImportResult(tabs = tabs, unresolvedRemoteIds = unresolvedRemoteIds)
    }

    fun importRemoteSessions(documents: List<DocumentSnapshot>): RemoteImportResult<Session> {
        return importRemoteItems(documents, ::toSession)
    }

    fun importRemoteTextNotes(documents: List<DocumentSnapshot>): RemoteImportResult<TextNote> {
        return importRemoteItems(documents, ::toTextNote)
    }

    suspend fun importRemoteAudioNotes(documents: List<DocumentSnapshot>): RemoteImportResult<AudioNote> {
        val audioNotes = mutableListOf<AudioNote>()
        val unresolvedRemoteIds = mutableSetOf<String>()

        documents.forEach { document ->
            val audioNote = toAudioNote(document)
            if (audioNote != null) {
                audioNotes += audioNote
            } else {
                unresolvedRemoteIds += document.id
            }
        }

        return RemoteImportResult(items = audioNotes, unresolvedRemoteIds = unresolvedRemoteIds)
    }

    fun canonicalTabIdentity(tab: TabItem): String {
        return if (tab.isUserTab) {
            "${tab.isUserTab}|${tab.name.trim().lowercase().replace(Regex("\\s+"), " ")}|${tab.lessonNumber}"
        } else {
            "builtin:${tab.id}"
        }
    }

    fun mergeTags(local: String, remote: String): String {
        return (local.split(",") + remote.split(","))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString(",")
    }

    fun sessionDocumentId(session: Session): String {
        return sha1(
            buildString {
                append(session.startTime.time)
                append('|')
                append(session.endTime.time)
                append('|')
                append(session.duration)
                append('|')
                append(session.practicedTabs.joinToString(";") { "${it.tabId}:${it.tabName}:${it.duration}" })
            }
        )
    }

    fun textNoteDocumentId(textNote: TextNote): String = sha1("${textNote.lessonId}|${textNote.createdAt.time}")

    fun audioNoteDocumentId(audioNote: AudioNote): String = sha1("${audioNote.lessonId}|${audioNote.createdAt.time}")

    fun goalFingerprint(goal: Goal): String {
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

    private fun practicedTabToFirestoreMap(practicedTab: PracticedTab): Map<String, Any> {
        return mapOf(
            "tabId" to practicedTab.tabId,
            "tabName" to practicedTab.tabName,
            "duration" to practicedTab.duration
        )
    }

    private fun <T> importRemoteItems(
        documents: List<DocumentSnapshot>,
        decode: (DocumentSnapshot) -> T?
    ): RemoteImportResult<T> {
        val items = mutableListOf<T>()
        val unresolvedRemoteIds = mutableSetOf<String>()

        documents.forEach { document ->
            val item = decode(document)
            if (item != null) {
                items += item
            } else {
                unresolvedRemoteIds += document.id
            }
        }

        return RemoteImportResult(items = items, unresolvedRemoteIds = unresolvedRemoteIds)
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
                    duration = map.getLongCompat("duration") ?: 0L
                )
            }
            Session(startTime = startTime, endTime = endTime, duration = duration, practicedTabs = practicedTabs)
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
                AudioNote(lessonId = lessonId, filePath = "", createdAt = createdAt)
            )
            val restoredPath = restoreAudioNoteFile(
                documentId,
                this["fileName"] as? String,
                this["storagePath"] as? String,
                this["fileBase64"] as? String
            ) ?: return null
            AudioNote(
                lessonId = lessonId,
                filePath = restoredPath,
                createdAt = createdAt,
                isFavorite = this["isFavorite"] as? Boolean ?: false
            )
        }.getOrNull()
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

    private fun sha1(value: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
