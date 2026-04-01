package com.example.thetest1.data.repository

import com.example.thetest1.data.remote.AiAssistantPromptBuilder
import com.example.thetest1.data.remote.AiAssistantConfigProvider
import com.example.thetest1.domain.model.AiAssistantRequest
import com.example.thetest1.domain.repository.AiAssistantRepository
import com.google.ai.client.generativeai.GenerativeModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiAssistantRepositoryImpl @Inject constructor(
    private val promptBuilder: AiAssistantPromptBuilder,
    private val configProvider: AiAssistantConfigProvider
) : AiAssistantRepository {

    private companion object {
        const val EmptyResponseError = "empty_response"
    }

    override suspend fun generateAnswer(request: AiAssistantRequest): String {
        val prompt = promptBuilder.build(request)
        val generativeModel = GenerativeModel(
            modelName = configProvider.getModelName(),
            apiKey = com.example.thetest1.BuildConfig.GEMINI_API_KEY
        )
        val response = generativeModel.generateContent(prompt)
        val text = response.text?.trim().orEmpty()
        if (text.isBlank()) {
            throw IllegalStateException(EmptyResponseError)
        }
        return text
    }
}
