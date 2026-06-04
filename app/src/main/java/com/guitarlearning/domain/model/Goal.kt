package com.guitarlearning.domain.model

import androidx.compose.runtime.Immutable
import java.util.Calendar
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
    val updatedAt: Long = 0L
) {
    val isCompleted: Boolean
        get() = progress >= target

    val isOverdue: Boolean
        get() {
            val deadlineCalendar = Calendar.getInstance().apply { timeInMillis = deadline }
            val todayCalendar = Calendar.getInstance()
            val isSameDay =
                deadlineCalendar.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR) &&
                    deadlineCalendar.get(Calendar.DAY_OF_YEAR) == todayCalendar.get(Calendar.DAY_OF_YEAR)
            return !isSameDay && System.currentTimeMillis() > deadline && !isCompleted
        }
}
