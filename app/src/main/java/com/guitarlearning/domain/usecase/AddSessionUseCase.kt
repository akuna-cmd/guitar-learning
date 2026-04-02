package com.guitarlearning.domain.usecase

import com.guitarlearning.domain.model.Session
import com.guitarlearning.domain.repository.SessionRepository

class AddSessionUseCase(private val sessionRepository: SessionRepository) {
    suspend operator fun invoke(session: Session) {
        sessionRepository.addSession(session)
    }
}
