package com.guitarlearning.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class TabItem(
    val id: String,
    val name: String,
    val description: String,
    val difficulty: Difficulty,
    val lessonNumber: Int,
    val isCompleted: Boolean = false,
    val isUserTab: Boolean = false,
    val filePath: String? = null,
    val asciiTabs: String? = null,
    val tags: List<String> = emptyList(),
    val folder: String = DEFAULT_TAB_FOLDER_KEY,
    val openCount: Int = 0,
    val lastOpenedAt: Long = 0L,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val offlineReady: Boolean = false
) {
    val tagsCsv: String
        get() = tags.joinToString(",")
}
