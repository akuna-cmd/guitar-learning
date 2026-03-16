package com.example.thetest1.presentation.ai_assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thetest1.R
import com.example.thetest1.domain.model.AiAssistantRequest
import com.example.thetest1.domain.usecase.AskAiAssistantUseCase
import com.example.thetest1.presentation.common.UiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Author {
    USER, AI
}

data class ChatMessage(
    val text: UiText,
    val author: Author
)

data class AiAssistantUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false
)

class AiAssistantViewModel(
    private val askAiAssistantUseCase: AskAiAssistantUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiAssistantUiState())
    val uiState: StateFlow<AiAssistantUiState> = _uiState.asStateFlow()

    fun askQuestion(question: String, theory: String, tabs: String, measureRange: IntRange? = null) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    messages = it.messages + ChatMessage(UiText.Plain(question), Author.USER)
                )
            }
            val result = askAiAssistantUseCase(
                AiAssistantRequest(
                    question = question,
                    theory = theory,
                    tabs = tabs,
                    measureRange = measureRange
                )
            )

            _uiState.update { current ->
                val message = result.fold(
                    onSuccess = { UiText.Plain(it) },
                    onFailure = { UiText.Res(R.string.ai_error_generic) }
                )
                current.copy(
                    isLoading = false,
                    messages = current.messages + ChatMessage(message, Author.AI)
                )
            }
        }
    }
}
