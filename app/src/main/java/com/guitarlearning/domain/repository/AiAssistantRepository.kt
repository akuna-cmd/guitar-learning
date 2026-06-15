package com.guitarlearning.domain.repository

import com.guitarlearning.domain.model.AiAssistantRequest
import com.guitarlearning.domain.settings.AiProvider

data class AiAnswerResult(
    val text: String,
    val backendLabel: String
)

interface AiAssistantRepository {
    suspend fun generateAnswer(request: AiAssistantRequest): AiAnswerResult
    suspend fun testConnection(provider: AiProvider, localServerUrl: String): String
    fun cancelTestConnection()
}
