package com.guitarlearning.data.sync

import com.guitarlearning.domain.settings.AiProvider
import com.guitarlearning.domain.settings.AppSettingsSnapshot
import com.guitarlearning.domain.model.AudioNote
import com.guitarlearning.domain.model.Goal
import com.guitarlearning.domain.model.GoalType
import com.guitarlearning.domain.model.PracticedTab
import com.guitarlearning.domain.model.Session
import com.guitarlearning.domain.model.TabItem
import com.guitarlearning.domain.model.TextNote
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
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

internal fun settingsSnapshotDefaults() = AppSettingsSnapshot()

internal fun AppSettingsSnapshot.toFirestoreMap(): Map<String, Any> {
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
        "updatedAt" to updatedAt
    )
}

internal fun Session.toFirestoreMap(): Map<String, Any> {
    return mapOf(
        "startTime" to startTime.time,
        "endTime" to endTime.time,
        "duration" to duration,
        "practicedTabs" to practicedTabs.map(PracticedTab::toFirestoreMap)
    )
}

internal fun TextNote.toFirestoreMap(): Map<String, Any> {
    return mapOf(
        "lessonId" to lessonId,
        "content" to content,
        "createdAt" to createdAt.time,
        "isFavorite" to isFavorite
    )
}

internal fun Goal.toFirestoreMap(): Map<String, Any> {
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

internal fun mergeCsvTags(local: String, remote: String): String {
    return (local.split(",") + remote.split(","))
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinct()
        .joinToString(",")
}

internal fun firestoreCanonicalTabIdentity(tab: TabItem): String {
    return if (tab.isUserTab) {
        "${tab.isUserTab}|${tab.name.trim().lowercase().replace(Regex("\\s+"), " ")}|${tab.lessonNumber}"
    } else {
        "builtin:${tab.id}"
    }
}

internal fun firestoreSessionDocumentId(session: Session): String {
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

internal fun firestoreTextNoteDocumentId(textNote: TextNote): String = sha1("${textNote.lessonId}|${textNote.createdAt.time}")

internal fun firestoreAudioNoteDocumentId(audioNote: AudioNote): String = sha1("${audioNote.lessonId}|${audioNote.createdAt.time}")

internal fun firestoreGoalFingerprint(goal: Goal): String {
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

internal fun <T> importRemoteItems(
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

internal fun DocumentSnapshot.toSettingsOrNull(): AppSettingsSnapshot? {
    if (!exists()) return null
    val defaults = settingsSnapshotDefaults()
    return AppSettingsSnapshot(
        themeMode = enumValueOrDefault(getString("themeMode"), defaults.themeMode),
        appLanguage = enumValueOrDefault(getString("appLanguage"), defaults.appLanguage),
        aiProvider = enumValueOrDefault(getString("aiProvider"), defaults.aiProvider),
        localAiServerUrl = getString("localAiServerUrl").orEmpty(),
        normalSpeed = getDouble("normalSpeed")?.toFloat() ?: 1.0f,
        practiceSpeed = getDouble("practiceSpeed")?.toFloat() ?: 0.25f,
        normalTabScale = getDouble("normalTabScale")?.toFloat() ?: 1.0f,
        practiceTabScale = getDouble("practiceTabScale")?.toFloat() ?: 1.0f,
        tabDisplayMode = enumValueOrDefault(getString("tabDisplayMode"), defaults.tabDisplayMode),
        updatedAt = getLong("updatedAt") ?: 0L
    )
}

internal fun DocumentSnapshot.decodeSessionOrNull(): Session? {
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
        Session(startTime = startTime, endTime = endTime, duration = duration, practicedTabs = practicedTabs)
    }.getOrNull()
}

internal fun DocumentSnapshot.decodeTextNoteOrNull(): TextNote? {
    return runCatching {
        val lessonId = getStringCompat("lessonId") ?: return null
        val createdAt = getDateCompat("createdAt") ?: return null
        TextNote(
            lessonId = lessonId,
            content = getStringCompat("content").orEmpty(),
            createdAt = createdAt,
            isFavorite = getBooleanCompat("isFavorite") ?: false
        )
    }.getOrNull()
}

internal fun DocumentSnapshot.decodeGoalOrNull(): Goal? {
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

internal fun DocumentSnapshot.decodeAiProviderOrDefault(field: String, fallback: AiProvider): AiProvider {
    return enumValueOrDefault(getString(field), fallback)
}

internal fun DocumentSnapshot.toSessionBackups(): List<Session> {
    val rawItems = get("sessionBackups") as? List<*> ?: return emptyList()
    return rawItems.mapNotNull { (it as? Map<*, *>)?.toSessionBackup() }
}

internal fun DocumentSnapshot.toTextNoteBackups(): List<TextNote> {
    val rawItems = get("textNoteBackups") as? List<*> ?: return emptyList()
    return rawItems.mapNotNull { (it as? Map<*, *>)?.toTextNoteBackup() }
}

internal suspend fun DocumentSnapshot.toAudioNoteBackups(
    restoreAudioNoteFile: suspend (documentId: String, fileName: String?, storagePath: String?, fileBase64: String?) -> String?
): List<AudioNote> {
    val rawItems = get("audioNoteBackups") as? List<*> ?: return emptyList()
    return rawItems.mapNotNull { (it as? Map<*, *>)?.toAudioNoteBackup(restoreAudioNoteFile) }
}

internal fun PracticedTab.toFirestoreMap(): Map<String, Any> {
    return mapOf(
        "tabId" to tabId,
        "tabName" to tabName,
        "duration" to duration
    )
}

internal fun Map<*, *>.toSessionBackup(): Session? {
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

internal fun Map<*, *>.toTextNoteBackup(): TextNote? {
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

internal suspend fun Map<*, *>.toAudioNoteBackup(
    restoreAudioNoteFile: suspend (documentId: String, fileName: String?, storagePath: String?, fileBase64: String?) -> String?
): AudioNote? {
    return runCatching {
        val lessonId = this["lessonId"] as? String ?: return null
        val createdAt = getDateCompat("createdAt") ?: return null
        val documentId = this["documentId"] as? String ?: firestoreAudioNoteDocumentId(
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

internal fun DocumentSnapshot.getDateCompat(field: String): Date? {
    return when (val value = get(field)) {
        is Date -> value
        is Timestamp -> value.toDate()
        is Number -> Date(value.toLong())
        is String -> value.toLongOrNull()?.let(::Date)
        else -> getDate(field) ?: getLong(field)?.let(::Date)
    }
}

internal fun DocumentSnapshot.getLongCompat(field: String): Long? {
    return when (val value = get(field)) {
        is Number -> value.toLong()
        is String -> value.toLongOrNull()
        is Date -> value.time
        is Timestamp -> value.toDate().time
        else -> getLong(field)
    }
}

internal fun DocumentSnapshot.getStringCompat(field: String): String? {
    return when (val value = get(field)) {
        null -> getString(field)
        is String -> value
        else -> value.toString()
    }
}

internal fun DocumentSnapshot.getBooleanCompat(field: String): Boolean? {
    return when (val value = get(field)) {
        is Boolean -> value
        is Number -> value.toInt() != 0
        is String -> value.equals("true", ignoreCase = true) || value == "1"
        else -> getBoolean(field)
    }
}

internal fun Map<*, *>.getDateCompat(field: String): Date? {
    return when (val value = this[field]) {
        is Date -> value
        is Timestamp -> value.toDate()
        is Number -> Date(value.toLong())
        is String -> value.toLongOrNull()?.let(::Date)
        else -> null
    }
}

internal fun Map<*, *>.getLongCompat(field: String): Long? {
    return when (val value = this[field]) {
        is Number -> value.toLong()
        is String -> value.toLongOrNull()
        is Date -> value.time
        is Timestamp -> value.toDate().time
        else -> null
    }
}

internal inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, fallback: T): T {
    return value?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: fallback
}

internal fun sha1(value: String): String {
    val digest = MessageDigest.getInstance("SHA-1").digest(value.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
}
