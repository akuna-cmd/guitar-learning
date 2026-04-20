package com.guitarlearning.presentation.tab_list

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guitarlearning.di.AppDispatchers
import com.guitarlearning.domain.model.DEFAULT_TAB_FOLDER_KEY
import com.guitarlearning.domain.model.Difficulty
import com.guitarlearning.domain.model.TabItem
import com.guitarlearning.domain.model.TabPlaybackProgress
import com.guitarlearning.domain.model.isDefaultTabFolder
import com.guitarlearning.domain.model.normalizeTabFolder
import com.guitarlearning.domain.repository.SoundFontRepository
import com.guitarlearning.domain.repository.TabFileRepository
import com.guitarlearning.domain.repository.TabPlaybackProgressRepository
import com.guitarlearning.domain.repository.TabRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TabListUiState(
    val tabs: List<TabItem> = emptyList(),
    val userTabs: List<TabItem> = emptyList(),
    val isTabsLoading: Boolean = true,
    val isUserTabsLoading: Boolean = true,
    val areMetricsLoading: Boolean = true,
    val selectedDifficulty: Difficulty = Difficulty.BEGINNER,
    val selectedTabIndex: Int = 0,
    val progressByTabId: Map<String, Int> = emptyMap(),
    val message: String? = null,
    val selectedFolder: String? = null,
    val customFolders: List<String> = emptyList(),
    val isDownloadingOfflinePackage: Boolean = false
) {
    fun displayFolder(tab: TabItem): String {
        return normalizeTabFolder(tab.folder)
    }

    val availableFolders: List<String>
        get() = listOf(DEFAULT_TAB_FOLDER_KEY) + (customFolders + userTabs.map { displayFolder(it) })
            .filterNot { isDefaultTabFolder(it) }
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
}

@HiltViewModel
class TabListViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tabRepository: TabRepository,
    private val progressRepository: TabPlaybackProgressRepository,
    private val tabFileRepository: TabFileRepository,
    private val soundFontRepository: SoundFontRepository,
    private val dispatchers: AppDispatchers
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
        val normalized = normalizeTabFolder(folderName)
        if (normalized.isEmpty() || isDefaultTabFolder(normalized)) return
        _uiState.update { current ->
            current.copy(
                customFolders = (current.customFolders + normalized).distinct().sorted(),
                selectedFolder = normalized
            )
        }
    }

    fun markTabOpened(tabId: String) {
        viewModelScope.launch(dispatchers.io) {
            tabRepository.markTabOpened(tabId)
        }
    }

    fun moveToFolder(tab: TabItem, folder: String) {
        viewModelScope.launch(dispatchers.io) {
            tabRepository.updateTabFolder(tab.id, folder)
        }
    }

    fun renameFolder(oldName: String, newName: String) {
        val target = normalizeTabFolder(newName).ifEmpty { return }
        if (isDefaultTabFolder(oldName) || isDefaultTabFolder(target)) return
        viewModelScope.launch(dispatchers.io) {
            val tabsToMove = _uiState.value.userTabs.filter { normalizeTabFolder(it.folder) == normalizeTabFolder(oldName) }
            tabsToMove.forEach { tab ->
                tabRepository.updateTabFolder(tab.id, target)
            }
            _uiState.update { current ->
                current.copy(
                    selectedFolder = if (current.selectedFolder == oldName) target else current.selectedFolder,
                    customFolders = current.customFolders.map { if (it == oldName) target else it }.distinct().sorted()
                )
            }
        }
    }

    fun deleteFolder(folderName: String) {
        if (isDefaultTabFolder(folderName)) return
        viewModelScope.launch(dispatchers.io) {
            val tabsToMove = _uiState.value.userTabs.filter { normalizeTabFolder(it.folder) == normalizeTabFolder(folderName) }
            tabsToMove.forEach { tab ->
                tabRepository.updateTabFolder(tab.id, DEFAULT_TAB_FOLDER_KEY)
            }
            _uiState.update { current ->
                current.copy(
                    selectedFolder = if (current.selectedFolder == folderName) null else current.selectedFolder,
                    customFolders = current.customFolders.filterNot { it == folderName }
                )
            }
        }
    }

    fun markOfflinePackage(tab: TabItem) {
        viewModelScope.launch(dispatchers.io) {
            _uiState.update { it.copy(isDownloadingOfflinePackage = true) }
            runCatching {
                tab.filePath?.takeIf { it.isNotBlank() }?.let { path ->
                    tabFileRepository.readTabBytes(path)
                }
                soundFontRepository.readSoundFontBytes()
                tabRepository.markOfflineReady(tab.id, true)
                val existingTags = tab.tagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                tabRepository.updateTabTags(tab.id, (existingTags + "offline").distinct())
            }
            _uiState.update { it.copy(isDownloadingOfflinePackage = false) }
        }
    }

    fun toggleCompleted(tabId: String) {
        viewModelScope.launch {
            _uiState.update { currentState ->
                val newTabs = currentState.tabs.map {
                    if (it.id == tabId) {
                        val updatedTab = it.copy(isCompleted = !it.isCompleted)
                        launch(dispatchers.io) { tabRepository.updateTab(updatedTab) }
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
                tabRepository.addUserTab(uri.toString())
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        message = error.message ?: context.getString(com.guitarlearning.R.string.user_tab_add_failed)
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
            tabRepository.deleteUserTab(tab)
        }
    }

    fun renameUserTab(tab: TabItem, newName: String) {
        viewModelScope.launch {
            tabRepository.renameUserTab(tab, newName)
        }
    }

    private fun observeTabs() {
        tabRepository.getTabs()
            .onEach { tabs ->
                _uiState.update { it.copy(tabs = tabs, isTabsLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    private fun observeUserTabs() {
        tabRepository.observeUserTabs()
            .onEach { userTabs ->
                _uiState.update { it.copy(userTabs = userTabs, isUserTabsLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    private fun observeTabMetrics() {
        viewModelScope.launch {
            progressRepository.observeAll().collect { progressList ->
                val progressMap = progressList.associate { progress ->
                    progress.tabId to calculateProgressPercent(progress)
                }
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
}
