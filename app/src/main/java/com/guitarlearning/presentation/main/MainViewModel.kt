package com.guitarlearning.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guitarlearning.di.AppDispatchers
import com.guitarlearning.domain.model.PracticedTab
import com.guitarlearning.domain.model.Session
import com.guitarlearning.domain.model.TabPlaybackProgress
import com.guitarlearning.domain.repository.SessionRepository
import com.guitarlearning.domain.usecase.ObserveContinueLearningUseCase
import com.guitarlearning.domain.usecase.ObserveHomeStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject

data class MainUiState(
    val isLoading: Boolean = true,
    val sessions: ImmutableList<Session> = persistentListOf(),
    val isSessionActive: Boolean = false,
    val sessionStartTime: Long = 0L,
    val sessionDuration: Long = 0L,
    val totalSessionTime: Long = 0L,
    val lessonsCompleted: Int = 0,
    val totalLessons: Int = 0,
    val userTabsCount: Int = 0,
    val practicedTabs: ImmutableList<PracticedTab> = persistentListOf(),
    val currentTab: PracticedTab? = null,
    val continueLessonId: String? = null
)

@HiltViewModel
class MainViewModel @Inject constructor(
    observeContinueLearningUseCase: ObserveContinueLearningUseCase,
    private val sessionRepository: SessionRepository,
    private val observeHomeStatsUseCase: ObserveHomeStatsUseCase,
    private val dispatchers: AppDispatchers
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val lastPlaybackProgress: StateFlow<TabPlaybackProgress?> = observeContinueLearningUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    private var timerJob: Job? = null
    private var tabTimerJob: Job? = null

    init {
        loadData()
    }

    private fun loadData() {
        observeHomeStatsUseCase()
            .onEach { stats ->
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        sessions = stats.sessions.toImmutableList(),
                        lessonsCompleted = stats.lessonsCompleted,
                        totalLessons = stats.totalLessons,
                        totalSessionTime = stats.totalSessionTime,
                        userTabsCount = stats.userTabsCount
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun startSession() {
        val startTime = System.currentTimeMillis()
        _uiState.update {
            it.copy(
                isSessionActive = true,
                sessionStartTime = startTime,
                practicedTabs = persistentListOf()
            )
        }
        timerJob = viewModelScope.launch {
            while (_uiState.value.isSessionActive) {
                _uiState.update { it.copy(sessionDuration = System.currentTimeMillis() - startTime) }
                delay(1000)
            }
        }
    }

    fun stopSession() {
        timerJob?.cancel()
        tabTimerJob?.cancel()

        val endTime = Date()
        val startTime = Date(_uiState.value.sessionStartTime)
        val currentPracticedTabs = _uiState.value.practicedTabs.toMutableList()

        _uiState.value.currentTab?.let {
            currentPracticedTabs.add(it)
        }

        viewModelScope.launch(dispatchers.io) {
            sessionRepository.addSession(
                Session(
                    startTime = startTime,
                    endTime = endTime,
                    practicedTabs = currentPracticedTabs
                )
            )
        }
        _uiState.update {
            it.copy(
                isSessionActive = false,
                sessionDuration = 0L,
                practicedTabs = persistentListOf(),
                currentTab = null
            )
        }
    }

    fun setActiveTab(tabId: String, tabName: String) {
        if (!_uiState.value.isSessionActive) return

        tabTimerJob?.cancel()

        _uiState.value.currentTab?.let {
            _uiState.update { uiState ->
                val updatedPracticedTabs = (uiState.practicedTabs + it).toImmutableList()
                uiState.copy(practicedTabs = updatedPracticedTabs)
            }
        }

        _uiState.update { it.copy(currentTab = PracticedTab(tabId, tabName, 0L)) }

        tabTimerJob = viewModelScope.launch {
            while (_uiState.value.isSessionActive) {
                delay(1000)
                _uiState.update { uiState ->
                    uiState.currentTab?.let {
                        uiState.copy(currentTab = it.copy(duration = it.duration + 1000))
                    } ?: uiState
                }
            }
        }
    }

    fun requestContinueLesson(tabId: String) {
        _uiState.update { it.copy(continueLessonId = tabId) }
    }

    fun consumeContinueLesson() {
        _uiState.update { it.copy(continueLessonId = null) }
    }
}
