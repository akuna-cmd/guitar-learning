package com.example.thetest1.domain.usecase

import com.example.thetest1.domain.model.AiAssistantRequest
import com.example.thetest1.domain.repository.AiAssistantRepository

class AskAiAssistantUseCase(private val repository: AiAssistantRepository) {
    suspend operator fun invoke(request: AiAssistantRequest): Result<String> {
        return runCatching { repository.generateAnswer(request) }
    }
}
