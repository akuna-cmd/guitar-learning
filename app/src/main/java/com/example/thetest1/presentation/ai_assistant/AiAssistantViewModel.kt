package com.example.thetest1.presentation.ai_assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thetest1.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Author {
    USER, AI
}

data class ChatMessage(
    val text: String,
    val author: Author
)

data class AiAssistantUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false
)

class AiAssistantViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AiAssistantUiState())
    val uiState: StateFlow<AiAssistantUiState> = _uiState.asStateFlow()

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash-lite",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    fun askQuestion(question: String, theory: String, tabs: String, measureRange: IntRange? = null) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    messages = it.messages + ChatMessage(question, Author.USER)
                )
            }
            try {
                val contextInstruction = if (measureRange != null) {
                    "УВАГА: Користувач питає КОНКРЕТНО про такти з ${measureRange.first} по ${measureRange.last}. Аналізуй ТІЛЬКИ ЦІ ТАКТИ і повністю ігноруй всі інші. Формат переданих табів: 'Beat X: [Струна Y [Назва] (лад Z), ...]' де Струна 1 - це найтонша 'e' струна (ВЕРХНЯ лінія на класичних малюнках табів), а Струна 6 - найтовща басова 'E' струна (НИЖНЯ лінія на табах). Ноти, згруповані в одних дужках 'Beat X: [...]', граються ОДНОЧАСНО (в один удар). Відповідай так, ніби інших тактів не існує."
                } else {
                    ""
                }

                val prompt = """
                Ти - високопрофейсійних викладач з гітари, який інтегрований в Android застосунок для навчання гри на гітарі. 
                На основі поданої теорії/табів дай відповіді на запитання учня. 
                Коротко (максимум 10-15 речень), професійно, нейтрально, без вступних слів, українською мовою.

                $contextInstruction

                Теорія:
                $theory

                Таби:
                $tabs

                Питання: 
                $question
                """
                val response = generativeModel.generateContent(prompt)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        messages = it.messages + ChatMessage(response.text ?: "", Author.AI)
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        messages = it.messages + ChatMessage("Error: ${e.message}", Author.AI)
                    )
                }
            }
        }
    }
}
