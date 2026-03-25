package com.example.thetest1.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.compose.runtime.Immutable

enum class GoalType {
    SESSION_TIME,
    LESSONS_COMPLETED,
    CUSTOM
}

@Immutable
@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val type: GoalType,
    val description: String,
    val target: Int,
    val progress: Int = 0,
    val deadline: Long,
    val isCompleted: Boolean = false,
    val isOverdue: Boolean = false
)
