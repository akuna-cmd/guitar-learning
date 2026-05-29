package com.guitarlearning.domain.model

import androidx.compose.runtime.Immutable
import java.util.UUID

enum class GoalType {
    SESSION_TIME,
    LESSONS_COMPLETED,
    CUSTOM
}

@Immutable
data class Goal(
    val id: Int = 0,
    val syncId: String = UUID.randomUUID().toString(),
    val type: GoalType,
    val description: String,
    val target: Int,
    val progress: Int = 0,
    val deadline: Long,
    val updatedAt: Long = 0L,
    val isCompleted: Boolean = false,
    val isOverdue: Boolean = false
)
