package com.example.thetest1.presentation.tab_list

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thetest1.domain.model.Difficulty
import com.example.thetest1.domain.model.TabItem
import com.example.thetest1.domain.usecase.AddUserTabUseCase
import com.example.thetest1.domain.usecase.DeleteUserTabUseCase
import com.example.thetest1.domain.usecase.GetTabsUseCase
import com.example.thetest1.domain.usecase.GetUserTabsUseCase
import com.example.thetest1.domain.usecase.RenameUserTabUseCase
import com.example.thetest1.domain.usecase.UpdateTabUseCase
import com.example.thetest1.domain.usecase.GetAllSessionsUseCase
import com.example.thetest1.domain.usecase.ObserveTabPlaybackProgressUseCase
import com.example.thetest1.domain.model.TabPlaybackProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TabListUiState(
    val tabs: List<TabItem> = emptyList(),
    val userTabs: List<TabItem> = emptyList(),
    val selectedDifficulty: Difficulty = Difficulty.BEGINNER,
    val selectedTabIndex: Int = 0,
    val progressByTabId: Map<String, Int> = emptyMap(),
    val lastSessionDurationByTabId: Map<String, Long> = emptyMap()
) {
    val filteredTabs: List<TabItem>
        get() = tabs.filter { it.difficulty == selectedDifficulty }

    val completedLessonsInSelectedDifficulty: Int
        get() = filteredTabs.count { it.isCompleted }

    val totalLessonsInSelectedDifficulty: Int
        get() = filteredTabs.size

    val totalCompletedLessons: Int
        get() = tabs.count { it.isCompleted }

    val totalLessons: Int
        get() = tabs.size

    val totalUserTabs: Int
        get() = userTabs.size
}

class TabListViewModel(
    private val getTabsUseCase: GetTabsUseCase,
    private val updateTabUseCase: UpdateTabUseCase,
    private val getUserTabsUseCase: GetUserTabsUseCase,
    private val addUserTabUseCase: AddUserTabUseCase,
    private val deleteUserTabUseCase: DeleteUserTabUseCase,
    private val renameUserTabUseCase: RenameUserTabUseCase,
    private val getAllSessionsUseCase: GetAllSessionsUseCase,
    private val observeTabPlaybackProgressUseCase: ObserveTabPlaybackProgressUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TabListUiState())
    val uiState: StateFlow<TabListUiState> = _uiState.asStateFlow()

    init {
        loadTabs()
        loadUserTabs()
        observeTabMetrics()
    }

    fun selectDifficulty(difficulty: Difficulty) {
        _uiState.update { it.copy(selectedDifficulty = difficulty) }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTabIndex = index) }
    }

    fun toggleCompleted(tabId: String) {
        viewModelScope.launch {
            _uiState.update { currentState ->
                val newTabs = currentState.tabs.map {
                    if (it.id == tabId) {
                        val updatedTab = it.copy(isCompleted = !it.isCompleted)
                        launch { updateTabUseCase(updatedTab) }
                        updatedTab
                    } else {
                        it
                    }
                }
                currentState.copy(tabs = newTabs)
            }
        }
    }

    fun addUserTab(uri: Uri) {
        viewModelScope.launch {
            addUserTabUseCase(uri.toString())
            loadUserTabs()
        }
    }

    fun deleteUserTab(tab: TabItem) {
        viewModelScope.launch {
            deleteUserTabUseCase(tab)
            loadUserTabs()
        }
    }

    fun renameUserTab(tab: TabItem, newName: String) {
        viewModelScope.launch {
            renameUserTabUseCase(tab, newName)
            loadUserTabs()
        }
    }

    private fun loadTabs() {
        viewModelScope.launch {
            getTabsUseCase().let { tabs ->
                _uiState.update { it.copy(tabs = tabs) }
            }
        }
    }

    private fun loadUserTabs() {
        viewModelScope.launch {
            _uiState.update { it.copy(userTabs = getUserTabsUseCase()) }
        }
    }

    private fun observeTabMetrics() {
        viewModelScope.launch {
            combine(
                getAllSessionsUseCase(),
                observeTabPlaybackProgressUseCase()
            ) { sessions, progressList ->
                val progressMap = progressList.associate { progress ->
                    progress.tabId to calculateProgressPercent(progress)
                }
                val lastSessionDurationMap = buildLastSessionDurationMap(sessions)
                progressMap to lastSessionDurationMap
            }.collect { (progressMap, lastSessionDurationMap) ->
                _uiState.update {
                    it.copy(
                        progressByTabId = progressMap,
                        lastSessionDurationByTabId = lastSessionDurationMap
                    )
                }
            }
        }
    }

    private fun calculateProgressPercent(progress: TabPlaybackProgress): Int {
        if (progress.totalBars <= 0) return 0
        return ((progress.lastBarIndex.toFloat() / progress.totalBars.toFloat()) * 100f).toInt()
            .coerceIn(0, 100)
    }

    private fun buildLastSessionDurationMap(sessions: List<com.example.thetest1.domain.model.Session>): Map<String, Long> {
        val sorted = sessions.sortedByDescending { it.startTime.time }
        val map = mutableMapOf<String, Long>()
        sorted.forEach { session ->
            session.practicedTabs.forEach { tab ->
                if (!map.containsKey(tab.tabId)) {
                    map[tab.tabId] = tab.duration
                }
            }
        }
        return map
    }
}
