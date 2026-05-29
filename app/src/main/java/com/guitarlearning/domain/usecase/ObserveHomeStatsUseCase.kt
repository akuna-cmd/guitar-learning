package com.guitarlearning.domain.usecase

import com.guitarlearning.domain.model.Session
import com.guitarlearning.domain.repository.SessionRepository
import com.guitarlearning.domain.repository.TabRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class HomeStats(
    val sessions: List<Session>,
    val totalSessionTime: Long,
    val lessonsCompleted: Int,
    val totalLessons: Int,
    val userTabsCount: Int
)

class ObserveHomeStatsUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val tabRepository: TabRepository
) {
    operator fun invoke(): Flow<HomeStats> {
        return combine(
            sessionRepository.getAllSessions(),
            tabRepository.getCompletedLessonsCount(),
            tabRepository.getTotalLessonsCount(),
            tabRepository.getUserTabsCount()
        ) { sessions, lessonsCompleted, totalLessons, userTabsCount ->
            HomeStats(
                sessions = sessions,
                totalSessionTime = sessions.sumOf { it.duration },
                lessonsCompleted = lessonsCompleted,
                totalLessons = totalLessons,
                userTabsCount = userTabsCount
            )
        }
    }
}
