package com.guitarlearning.domain.usecase

import com.guitarlearning.domain.model.Goal
import com.guitarlearning.domain.model.GoalType
import com.guitarlearning.domain.repository.GoalRepository
import com.guitarlearning.domain.repository.SessionRepository
import com.guitarlearning.domain.repository.TabRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveGoalsProgressUseCase @Inject constructor(
    private val goalRepository: GoalRepository,
    private val tabRepository: TabRepository,
    private val sessionRepository: SessionRepository
) {
    private companion object {
        const val MillisInMinute = 60_000L
    }

    operator fun invoke(): Flow<List<Goal>> {
        return combine(
            goalRepository.getGoals(),
            tabRepository.getCompletedLessonsCount(),
            sessionRepository.getAllSessions()
        ) { goals, completedLessons, sessions ->
            goals.map { goal ->
                val progress = when (goal.type) {
                    GoalType.LESSONS_COMPLETED -> completedLessons
                    GoalType.SESSION_TIME -> (sessions.sumOf { it.duration } / MillisInMinute).toInt()
                    GoalType.CUSTOM -> goal.progress
                }
                goal.copy(progress = progress)
            }
        }
    }
}
