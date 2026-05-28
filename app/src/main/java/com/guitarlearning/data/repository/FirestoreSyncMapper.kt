package com.guitarlearning.data.repository

import android.util.Log
import com.guitarlearning.domain.model.AudioNote
import com.guitarlearning.domain.model.DEFAULT_TAB_FOLDER_KEY
import com.guitarlearning.domain.model.Difficulty
import com.guitarlearning.domain.model.Goal
import com.guitarlearning.domain.model.GoalType
import com.guitarlearning.domain.model.TabItem
import com.guitarlearning.domain.model.TabPlaybackProgress
import com.guitarlearning.domain.model.TextNote
import com.guitarlearning.domain.model.normalizeTabFolder
import com.google.firebase.firestore.DocumentSnapshot
import java.io.File

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
    fun settingsToFirestoreMap(settings: com.guitarlearning.data.settings.AppSettingsSnapshot): Map<String, Any> {
        return settings.toFirestoreMap()
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

    fun sessionToFirestoreMap(session: com.guitarlearning.domain.model.Session): Map<String, Any> {
        return session.toFirestoreMap()
    }

    fun textNoteToFirestoreMap(textNote: TextNote): Map<String, Any> {
        return textNote.toFirestoreMap()
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
        return goal.toFirestoreMap()
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

    fun toSettings(document: DocumentSnapshot) = document.toSettingsOrNull()

    fun toSession(document: DocumentSnapshot) = document.decodeSessionOrNull().alsoLogFailure(document, "toSession")

    fun toTextNote(document: DocumentSnapshot) = document.decodeTextNoteOrNull().alsoLogFailure(document, "toTextNote")

    fun toSessionBackups(document: DocumentSnapshot) = document.toSessionBackups()

    fun toTextNoteBackups(document: DocumentSnapshot) = document.toTextNoteBackups()

    suspend fun toAudioNoteBackups(document: DocumentSnapshot) = document.toAudioNoteBackups(restoreAudioNoteFile)

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
        return document.decodeGoalOrNull()
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

    fun importRemoteSessions(documents: List<DocumentSnapshot>) = importRemoteItems(documents, ::toSession)

    fun importRemoteTextNotes(documents: List<DocumentSnapshot>) = importRemoteItems(documents, ::toTextNote)

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

    fun canonicalTabIdentity(tab: TabItem): String = firestoreCanonicalTabIdentity(tab)

    fun mergeTags(local: String, remote: String): String = mergeCsvTags(local, remote)

    fun sessionDocumentId(session: com.guitarlearning.domain.model.Session): String = firestoreSessionDocumentId(session)

    fun textNoteDocumentId(textNote: TextNote): String = firestoreTextNoteDocumentId(textNote)

    fun audioNoteDocumentId(audioNote: AudioNote): String = firestoreAudioNoteDocumentId(audioNote)

    fun goalFingerprint(goal: Goal): String = firestoreGoalFingerprint(goal)

    private fun <T> T?.alsoLogFailure(document: DocumentSnapshot, operation: String): T? {
        if (this != null) return this
        Log.w(logTag, "$operation:unreadable docId=${document.id} data=${document.data.orEmpty()}")
        return null
    }
}
