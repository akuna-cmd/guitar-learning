package com.example.thetest1.domain.usecase

import com.example.thetest1.domain.model.Session
import com.example.thetest1.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.Date

class GetSessionsForLastMonthUseCase(private val sessionRepository: SessionRepository) {
    operator fun invoke(): Flow<List<Session>> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -1)
        return sessionRepository.getSessionsSince(calendar.time)
    }
}
