package com.example.thetest1.presentation.tab_list

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thetest1.domain.model.Difficulty
import com.example.thetest1.domain.model.TabItem
import com.example.thetest1.domain.usecase.AddUserTabUseCase
import com.example.thetest1.domain.usecase.DeleteUserTabUseCase
import com.example.thetest1.domain.usecase.GetSoundFontBytesUseCase
import com.example.thetest1.domain.usecase.GetTabFileBytesUseCase
import com.example.thetest1.domain.usecase.GetTabsUseCase
import com.example.thetest1.domain.usecase.GetUserTabsUseCase
import com.example.thetest1.domain.usecase.MarkTabOfflineReadyUseCase
import com.example.thetest1.domain.usecase.MarkTabOpenedUseCase
import com.example.thetest1.domain.usecase.RenameUserTabUseCase
import com.example.thetest1.domain.usecase.UpdateTabUseCase
import com.example.thetest1.domain.usecase.UpdateTabFolderUseCase
import com.example.thetest1.domain.usecase.UpdateTabTagsUseCase
import com.example.thetest1.domain.usecase.GetAllSessionsUseCase
import com.example.thetest1.domain.usecase.ObserveTabPlaybackProgressUseCase
import com.example.thetest1.domain.usecase.ObserveTabsUseCase
import com.example.thetest1.domain.usecase.ObserveUserTabsUseCase
import com.example.thetest1.domain.model.TabPlaybackProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

data class TabListUiState(
    val tabs: List<TabItem> = emptyList(),
    val userTabs: List<TabItem> = emptyList(),
    val isTabsLoading: Boolean = true,
    val isUserTabsLoading: Boolean = true,
    val areMetricsLoading: Boolean = true,
    val selectedDifficulty: Difficulty = Difficulty.BEGINNER,
    val selectedTabIndex: Int = 0,
    val progressByTabId: Map<String, Int> = emptyMap(),
    val lastSessionDurationByTabId: Map<String, Long> = emptyMap(),
    val message: String? = null,
    val selectedFolder: String? = null,
    val customFolders: List<String> = emptyList(),
    val isDownloadingOfflinePackage: Boolean = false
) {
    private fun tags(tab: TabItem): Set<String> =
        tab.tagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()

    fun displayFolder(tab: TabItem): String {
        return tab.folder.trim().ifEmpty { "Без папки" }
    }

    val availableFolders: List<String>
        get() = listOf("Без папки") + (customFolders + userTabs.map { displayFolder(it) })
            .filter { it != "Без папки" }
            .distinct()
            .sorted()

    val filteredTabs: List<TabItem>
        get() = tabs.filter { it.difficulty == selectedDifficulty }.sortedBy { it.lessonNumber }

    val filteredUserTabs: List<TabItem>
        get() = applyLibraryRules(userTabs, applyFolderFilter = true)

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

    private fun applyLibraryRules(
        source: List<TabItem>,
        applyFolderFilter: Boolean = false
    ): List<TabItem> {
        val filtered = source.filter { tab ->
            val folderMatches = !applyFolderFilter || selectedFolder == null || displayFolder(tab) == selectedFolder
            folderMatches
        }
        return filtered
    }

    fun isOfflineAvailable(tab: TabItem): Boolean {
        if (!tab.isUserTab) return true
        return tab.offlineReady || tags(tab).contains("offline")
    }
}

class TabListViewModel(
    private val getTabsUseCase: GetTabsUseCase,
    private val observeTabsUseCase: ObserveTabsUseCase,
    private val updateTabUseCase: UpdateTabUseCase,
    private val getUserTabsUseCase: GetUserTabsUseCase,
    private val observeUserTabsUseCase: ObserveUserTabsUseCase,
    private val addUserTabUseCase: AddUserTabUseCase,
    private val markTabOpenedUseCase: MarkTabOpenedUseCase,
    private val updateTabFolderUseCase: UpdateTabFolderUseCase,
    private val updateTabTagsUseCase: UpdateTabTagsUseCase,
    private val markTabOfflineReadyUseCase: MarkTabOfflineReadyUseCase,
    private val deleteUserTabUseCase: DeleteUserTabUseCase,
    private val renameUserTabUseCase: RenameUserTabUseCase,
    private val getAllSessionsUseCase: GetAllSessionsUseCase,
    private val observeTabPlaybackProgressUseCase: ObserveTabPlaybackProgressUseCase,
    private val getTabFileBytesUseCase: GetTabFileBytesUseCase,
    private val getSoundFontBytesUseCase: GetSoundFontBytesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TabListUiState())
    val uiState: StateFlow<TabListUiState> = _uiState.asStateFlow()

    init {
        observeTabs()
        observeUserTabs()
        observeTabMetrics()
    }

    fun selectDifficulty(difficulty: Difficulty) {
        _uiState.update { it.copy(selectedDifficulty = difficulty) }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTabIndex = index) }
    }

    fun selectFolder(folder: String?) {
        _uiState.update { it.copy(selectedFolder = folder) }
    }

    fun createFolder(folderName: String) {
        val normalized = folderName.trim()
        if (normalized.isEmpty() || normalized == "Без папки") return
        _uiState.update { current ->
            current.copy(
                customFolders = (current.customFolders + normalized).distinct().sorted(),
                selectedFolder = normalized
            )
        }
    }

    fun markTabOpened(tabId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            markTabOpenedUseCase(tabId)
            loadTabs()
            loadUserTabs()
        }
    }

    fun moveToFolder(tab: TabItem, folder: String) {
        viewModelScope.launch(Dispatchers.IO) {
            updateTabFolderUseCase(tab.id, folder)
            loadTabs()
            loadUserTabs()
        }
    }

    fun renameFolder(oldName: String, newName: String) {
        val target = newName.trim().ifEmpty { return }
        if (oldName == "Без папки" || target == "Без папки") return
        viewModelScope.launch(Dispatchers.IO) {
            val tabsToMove = _uiState.value.userTabs.filter { it.folder == oldName }
            tabsToMove.forEach { tab ->
                updateTabFolderUseCase(tab.id, target)
            }
            _uiState.update { current ->
                current.copy(
                    selectedFolder = if (current.selectedFolder == oldName) target else current.selectedFolder,
                    customFolders = current.customFolders.map { if (it == oldName) target else it }.distinct().sorted()
                )
            }
            loadUserTabs()
        }
    }

    fun deleteFolder(folderName: String) {
        if (folderName == "Без папки") return
        viewModelScope.launch(Dispatchers.IO) {
            val tabsToMove = _uiState.value.userTabs.filter { it.folder == folderName }
            tabsToMove.forEach { tab ->
                updateTabFolderUseCase(tab.id, "Без папки")
            }
            _uiState.update { current ->
                current.copy(
                    selectedFolder = if (current.selectedFolder == folderName) null else current.selectedFolder,
                    customFolders = current.customFolders.filterNot { it == folderName }
                )
            }
            loadUserTabs()
        }
    }

    fun markOfflinePackage(tab: TabItem) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isDownloadingOfflinePackage = true) }
            runCatching {
                tab.filePath?.takeIf { it.isNotBlank() }?.let { path ->
                    getTabFileBytesUseCase(path)
                }
                getSoundFontBytesUseCase()
                markTabOfflineReadyUseCase(tab.id, true)
                val existingTags = tab.tagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                updateTabTagsUseCase(tab.id, (existingTags + "offline").distinct())
            }
            _uiState.update { it.copy(isDownloadingOfflinePackage = false) }
            loadTabs()
            loadUserTabs()
        }
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
            _uiState.update { it.copy(message = null) }
            runCatching {
                addUserTabUseCase(uri.toString())
            }.onSuccess {
                loadUserTabs()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        message = error.message ?: "Не вдалося додати табулатуру"
                    )
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
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
        viewModelScope.launch(Dispatchers.IO) {
            getTabsUseCase().let { tabs ->
                _uiState.update { it.copy(tabs = tabs, isTabsLoading = false) }
            }
        }
    }

    private fun loadUserTabs() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(userTabs = getUserTabsUseCase(), isUserTabsLoading = false) }
        }
    }

    private fun observeTabs() {
        observeTabsUseCase()
            .onEach { tabs ->
                _uiState.update { it.copy(tabs = tabs, isTabsLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    private fun observeUserTabs() {
        observeUserTabsUseCase()
            .onEach { userTabs ->
                _uiState.update { it.copy(userTabs = userTabs, isUserTabsLoading = false) }
            }
            .launchIn(viewModelScope)
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
                _uiState.update { current ->
                    val mergedProgress = if (
                        progressMap.isEmpty() &&
                        current.progressByTabId.isNotEmpty()
                    ) {
                        // Prevent a transient 0% flicker while DataStore/flows are re-emitting.
                        current.progressByTabId
                    } else {
                        progressMap
                    }
                    current.copy(
                        progressByTabId = mergedProgress,
                        lastSessionDurationByTabId = lastSessionDurationMap,
                        areMetricsLoading = false
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
