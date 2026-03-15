package com.example.thetest1.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thetest1.domain.model.PracticedTab
import com.example.thetest1.domain.model.Session
import com.example.thetest1.domain.usecase.AddSessionUseCase
import com.example.thetest1.domain.usecase.GetAllSessionsUseCase
import com.example.thetest1.domain.usecase.GetCompletedLessonsCountUseCase
import com.example.thetest1.domain.usecase.GetTotalLessonsCountUseCase
import com.example.thetest1.domain.usecase.GetUserTabsCountUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date

data class MainUiState(
    val sessions: List<Session> = emptyList(),
    val isSessionActive: Boolean = false,
    val sessionStartTime: Long = 0L,
    val sessionDuration: Long = 0L,
    val totalSessionTime: Long = 0L,
    val lessonsCompleted: Int = 0,
    val totalLessons: Int = 0,
    val userTabsCount: Int = 0,
    val practicedTabs: List<PracticedTab> = emptyList(),
    val currentTab: PracticedTab? = null,
    val showBottomBar: Boolean = true
)

class MainViewModel(
    private val getAllSessionsUseCase: GetAllSessionsUseCase,
    private val addSessionUseCase: AddSessionUseCase,
    private val getCompletedLessonsCountUseCase: GetCompletedLessonsCountUseCase,
    private val getTotalLessonsCountUseCase: GetTotalLessonsCountUseCase,
    private val getUserTabsCountUseCase: GetUserTabsCountUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var tabTimerJob: Job? = null

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                getAllSessionsUseCase(),
                getCompletedLessonsCountUseCase(),
                getTotalLessonsCountUseCase(),
                getUserTabsCountUseCase()
            ) { sessions, lessonsCompleted, totalLessons, userTabsCount ->
                val totalSessionTime = sessions.sumOf { it.duration }
                _uiState.update {
                    it.copy(
                        sessions = sessions,
                        lessonsCompleted = lessonsCompleted,
                        totalLessons = totalLessons,
                        totalSessionTime = totalSessionTime,
                        userTabsCount = userTabsCount
                    )
                }
            }.collect {}
        }
    }

    fun startSession() {
        val startTime = System.currentTimeMillis()
        _uiState.update {
            it.copy(
                isSessionActive = true,
                sessionStartTime = startTime,
                practicedTabs = emptyList()
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
        val duration = _uiState.value.sessionDuration
        val practicedTabs = _uiState.value.practicedTabs.toMutableList()

        _uiState.value.currentTab?.let {
            practicedTabs.add(it)
        }

        viewModelScope.launch {
            addSessionUseCase(
                Session(
                    startTime = startTime,
                    endTime = endTime,
                    duration = duration,
                    practicedTabs = practicedTabs
                )
            )
        }
        _uiState.update {
            it.copy(
                isSessionActive = false,
                sessionDuration = 0L,
                practicedTabs = emptyList(),
                currentTab = null
            )
        }
    }

    fun setActiveTab(tabId: String, tabName: String) {
        if (!_uiState.value.isSessionActive) return

        tabTimerJob?.cancel()

        _uiState.value.currentTab?.let {
            _uiState.update { uiState ->
                val updatedPracticedTabs = uiState.practicedTabs.toMutableList()
                updatedPracticedTabs.add(it)
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

    fun setShowBottomBar(show: Boolean) {
        _uiState.update { it.copy(showBottomBar = show) }
    }
}
