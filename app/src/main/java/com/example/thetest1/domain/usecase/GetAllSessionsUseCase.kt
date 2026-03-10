package com.example.thetest1.domain.usecase

import com.example.thetest1.domain.model.Session
import com.example.thetest1.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow

class GetAllSessionsUseCase(private val sessionRepository: SessionRepository) {
    operator fun invoke(): Flow<List<Session>> {
        return sessionRepository.getAllSessions()
    }
}