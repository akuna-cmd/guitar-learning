package com.example.thetest1.domain.usecase

import com.example.thetest1.domain.model.Goal
import com.example.thetest1.domain.model.GoalType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar

class ObserveGoalsProgressUseCase(
    private val getGoalsUseCase: GetGoalsUseCase,
    private val getCompletedLessonsCountUseCase: GetCompletedLessonsCountUseCase,
    private val getAllSessionsUseCase: GetAllSessionsUseCase
) {
    private companion object {
        const val MillisInMinute = 60_000L
    }

    operator fun invoke(): Flow<List<Goal>> {
        return combine(
            getGoalsUseCase(),
            getCompletedLessonsCountUseCase(),
            getAllSessionsUseCase()
        ) { goals, completedLessons, sessions ->
            goals.map { goal ->
                val progress = when (goal.type) {
                    GoalType.LESSONS_COMPLETED -> completedLessons
                    GoalType.SESSION_TIME -> (sessions.sumOf { it.duration } / MillisInMinute).toInt()
                    GoalType.CUSTOM -> if (goal.isCompleted) 1 else 0
                }

                val isCompleted =
                    if (goal.type == GoalType.CUSTOM) goal.isCompleted else progress >= goal.target

                val deadlineCalendar =
                    Calendar.getInstance().apply { timeInMillis = goal.deadline }
                val todayCalendar = Calendar.getInstance()

                val isSameDay =
                    deadlineCalendar.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR) &&
                        deadlineCalendar.get(Calendar.DAY_OF_YEAR) == todayCalendar.get(Calendar.DAY_OF_YEAR)

                val isOverdue = !isSameDay && System.currentTimeMillis() > goal.deadline && !isCompleted

                goal.copy(progress = progress, isCompleted = isCompleted, isOverdue = isOverdue)
            }
        }
    }
}
