package com.example.thetest1.domain.usecase

import com.example.thetest1.domain.model.Session
import com.example.thetest1.domain.repository.SessionRepository

class AddSessionUseCase(private val sessionRepository: SessionRepository) {
    suspend operator fun invoke(session: Session) {
        sessionRepository.addSession(session)
    }
}
