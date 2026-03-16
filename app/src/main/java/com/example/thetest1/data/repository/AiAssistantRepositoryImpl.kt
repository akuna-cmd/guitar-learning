package com.example.thetest1.data.repository

import com.example.thetest1.data.remote.AiAssistantPromptBuilder
import com.example.thetest1.domain.model.AiAssistantRequest
import com.example.thetest1.domain.repository.AiAssistantRepository
import com.google.ai.client.generativeai.GenerativeModel

class AiAssistantRepositoryImpl(
    private val generativeModel: GenerativeModel
) : AiAssistantRepository {

    private companion object {
        const val EmptyResponseError = "empty_response"
    }

    override suspend fun generateAnswer(request: AiAssistantRequest): String {
        val prompt = AiAssistantPromptBuilder.build(request)
        val response = generativeModel.generateContent(prompt)
        val text = response.text?.trim().orEmpty()
        if (text.isBlank()) {
            throw IllegalStateException(EmptyResponseError)
        }
        return text
    }
}
