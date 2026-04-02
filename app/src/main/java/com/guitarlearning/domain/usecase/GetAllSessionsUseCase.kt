package com.guitarlearning.domain.usecase

import com.guitarlearning.domain.model.Session
import com.guitarlearning.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow

class GetAllSessionsUseCase(private val sessionRepository: SessionRepository) {
    operator fun invoke(): Flow<List<Session>> {
        return sessionRepository.getAllSessions()
    }
}
