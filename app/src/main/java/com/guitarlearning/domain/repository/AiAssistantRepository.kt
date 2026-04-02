package com.guitarlearning.domain.repository

import com.guitarlearning.domain.model.AiAssistantRequest

interface AiAssistantRepository {
    suspend fun generateAnswer(request: AiAssistantRequest): String
}
