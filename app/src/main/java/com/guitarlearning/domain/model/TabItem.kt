package com.guitarlearning.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.compose.runtime.Immutable

@Immutable
@Entity(tableName = "tabs")
data class TabItem(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val difficulty: Difficulty,
    val lessonNumber: Int,
    val isCompleted: Boolean = false,
    val isUserTab: Boolean = false,
    val filePath: String? = null,
    val asciiTabs: String? = null,
    val tagsCsv: String = "",
    val folder: String = DEFAULT_TAB_FOLDER_KEY,
    val openCount: Int = 0,
    val lastOpenedAt: Long = 0L,
    val updatedAt: Long = 0L,
    val offlineReady: Boolean = false
)
