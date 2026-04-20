package com.guitarlearning.domain.usecase

import com.guitarlearning.domain.model.AiAssistantRequest
import com.guitarlearning.domain.repository.AiAnswerResult
import com.guitarlearning.domain.repository.AiAssistantRepository

class AskAiAssistantUseCase(private val repository: AiAssistantRepository) {
    suspend operator fun invoke(request: AiAssistantRequest): Result<AiAnswerResult> {
        return runCatching { repository.generateAnswer(request) }
    }
}
