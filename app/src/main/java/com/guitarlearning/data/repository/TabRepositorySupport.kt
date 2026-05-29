package com.guitarlearning.data.repository

import com.guitarlearning.data.model.LessonDto
import com.guitarlearning.domain.model.DEFAULT_TAB_FOLDER_KEY
import com.guitarlearning.domain.model.Difficulty
import com.guitarlearning.domain.model.TabItem

internal const val DefaultUserTabFileExtension = "gp"
internal val SupportedUserTabExtensions = setOf("gp", "gp3", "gp4", "gp5", "gpx")

internal fun buildBuiltInTabs(
    lessons: List<LessonDto>,
    useEnglishDescriptions: Boolean
): List<TabItem> {
    return lessons.mapIndexed { index, lesson ->
        TabItem(
            id = lesson.id,
            name = lesson.title,
            description = lesson.localizedDescription(useEnglishDescriptions),
            difficulty = lesson.level.toDifficulty(),
            lessonNumber = index + 1,
            isCompleted = false,
            isUserTab = false,
            tagsCsv = "${lesson.level},lesson",
            folder = DEFAULT_TAB_FOLDER_KEY,
            updatedAt = 0L,
            offlineReady = true
        )
    }
}

internal fun localizeBuiltInTabs(
    storedTabs: List<TabItem>,
    lessonDescriptionsById: Map<String, String>
): List<TabItem> {
    return storedTabs.map { tab ->
        val localizedDescription = lessonDescriptionsById[tab.id] ?: return@map tab
        if (tab.description == localizedDescription) {
            tab
        } else {
            tab.copy(description = localizedDescription)
        }
    }
}

internal fun normalizeTags(tags: List<String>): String {
    return tags
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinct()
        .joinToString(",")
}

internal fun fallbackDisplayNameFromPath(path: String?): String? {
    return path
        ?.substringAfterLast('/', missingDelimiterValue = path)
        ?.takeIf(String::isNotBlank)
}

private fun String.toDifficulty(): Difficulty {
    return when (lowercase()) {
        "intermediate" -> Difficulty.INTERMEDIATE
        "advanced" -> Difficulty.ADVANCED
        else -> Difficulty.BEGINNER
    }
}
