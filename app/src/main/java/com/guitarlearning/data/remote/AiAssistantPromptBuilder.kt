package com.guitarlearning.data.remote

import android.content.Context
import com.guitarlearning.R
import com.guitarlearning.domain.model.AiAssistantRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiAssistantPromptBuilder @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun build(request: AiAssistantRequest): String {
        val contextInstruction = request.measureRange?.let { range ->
            context.getString(
                R.string.ai_prompt_measure_range_template,
                range.first,
                range.last
            )
        }.orEmpty()

        return context.getString(
            R.string.ai_prompt_template,
            context.getString(R.string.ai_prompt_role_instruction),
            context.getString(R.string.ai_prompt_output_instruction),
            contextInstruction,
            request.theory,
            request.tabs,
            request.question
        ).trimIndent()
    }
}
