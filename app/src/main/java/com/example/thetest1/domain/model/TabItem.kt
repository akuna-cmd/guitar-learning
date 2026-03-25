package com.example.thetest1.domain.model

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
    val asciiTabs: String? = null
)
