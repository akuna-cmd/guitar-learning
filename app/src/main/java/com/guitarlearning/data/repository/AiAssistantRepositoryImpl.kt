package com.guitarlearning.data.repository

import com.guitarlearning.data.remote.AiAssistantPromptBuilder
import com.guitarlearning.data.remote.AiAssistantConfigProvider
import com.guitarlearning.domain.model.AiAssistantRequest
import com.guitarlearning.domain.repository.AiAssistantRepository
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
            apiKey = com.guitarlearning.BuildConfig.GEMINI_API_KEY
        )
        val response = generativeModel.generateContent(prompt)
        val text = response.text?.trim().orEmpty()
        if (text.isBlank()) {
            throw IllegalStateException(EmptyResponseError)
        }
        return text
    }
}
