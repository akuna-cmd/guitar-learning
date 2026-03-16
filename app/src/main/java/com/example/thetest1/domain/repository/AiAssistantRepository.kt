package com.example.thetest1.domain.repository

import com.example.thetest1.domain.model.AiAssistantRequest

interface AiAssistantRepository {
    suspend fun generateAnswer(request: AiAssistantRequest): String
}
