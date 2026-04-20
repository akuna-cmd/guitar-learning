package com.guitarlearning.presentation.ai_assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guitarlearning.R
import com.guitarlearning.domain.model.AiAssistantRequest
import com.guitarlearning.domain.repository.AiAssistantRepository
import com.guitarlearning.presentation.ui.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class Author {
    USER, AI
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: UiText,
    val author: Author,
    val sourceLabel: UiText? = null
)

data class AiAssistantUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class AiAssistantViewModel @Inject constructor(
    private val aiAssistantRepository: AiAssistantRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiAssistantUiState())
    val uiState: StateFlow<AiAssistantUiState> = _uiState.asStateFlow()

    fun askQuestion(question: String, theory: String, tabs: String, measureRange: IntRange? = null) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    messages = it.messages + ChatMessage(text = UiText.Plain(question), author = Author.USER)
                )
            }
            val result = runCatching {
                aiAssistantRepository.generateAnswer(
                    AiAssistantRequest(
                        question = question,
                        theory = theory,
                        tabs = tabs,
                        measureRange = measureRange
                    )
                )
            }

            _uiState.update { current ->
                val message = result.fold(
                    onSuccess = { UiText.Plain(it.text) },
                    onFailure = { error ->
                        val text = error.localizedMessage?.trim().orEmpty()
                        if (text.isNotBlank()) UiText.Plain(text) else UiText.Res(R.string.ai_error_generic)
                    }
                )
                val sourceLabel = result.getOrNull()?.backendLabel?.let(UiText::Plain)
                current.copy(
                    isLoading = false,
                    messages = current.messages + ChatMessage(
                        text = message,
                        author = Author.AI,
                        sourceLabel = sourceLabel
                    )
                )
            }
        }
    }
}
