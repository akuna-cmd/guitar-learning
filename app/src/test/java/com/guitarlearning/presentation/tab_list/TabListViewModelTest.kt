package com.guitarlearning.presentation.tab_list

import com.guitarlearning.di.AppDispatchers
import com.guitarlearning.domain.model.TabPlaybackProgress
import com.guitarlearning.testutil.FakeSoundFontRepository
import com.guitarlearning.testutil.FakeTabFileRepository
import com.guitarlearning.testutil.FakeTabPlaybackProgressRepository
import com.guitarlearning.testutil.FakeTabRepository
import com.guitarlearning.testutil.MainDispatcherRule
import com.guitarlearning.testutil.testTabItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TabListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun renameFolder_updatesAllTabsInFolderAndSwitchesActiveFilter() = runTest {
        val tabRepository = FakeTabRepository(
            initialUserTabs = listOf(
                testTabItem(id = "u1", folder = "Rock", isUserTab = true),
                testTabItem(id = "u2", folder = "Rock", isUserTab = true),
                testTabItem(id = "u3", folder = "Jazz", isUserTab = true)
            )
        )
        val viewModel = createViewModel(tabRepository, FakeTabPlaybackProgressRepository())

        advanceUntilIdle()
        viewModel.createFolder("Rock")
        viewModel.selectFolder("Rock")
        viewModel.renameFolder("Rock", "Favorites")
        advanceUntilIdle()

        assertEquals(listOf("u1" to "Favorites", "u2" to "Favorites"), tabRepository.folderUpdates)
        assertEquals("Favorites", viewModel.uiState.value.selectedFolder)
        assertEquals(listOf("Favorites"), viewModel.uiState.value.customFolders)
    }

    @Test
    fun keepsLastProgressWhenRepositoryTemporarilyEmitsEmptyState() = runTest {
        val progressRepository = FakeTabPlaybackProgressRepository(
            initialProgress = listOf(
                TabPlaybackProgress(
                    tabId = "lesson-1",
                    tabName = "Etude",
                    lastTick = 400L,
                    lastBarIndex = 5,
                    totalBars = 10,
                    updatedAt = 1_000L
                )
            )
        )
        val viewModel = createViewModel(FakeTabRepository(), progressRepository)

        advanceUntilIdle()
        assertEquals(50, viewModel.uiState.value.progressByTabId["lesson-1"])

        progressRepository.progressFlow.value = emptyList()
        advanceUntilIdle()

        assertEquals(50, viewModel.uiState.value.progressByTabId["lesson-1"])
    }

    private fun createViewModel(
        tabRepository: FakeTabRepository,
        progressRepository: FakeTabPlaybackProgressRepository
    ): TabListViewModel {
        return TabListViewModel(
            tabRepository = tabRepository,
            progressRepository = progressRepository,
            tabFileRepository = FakeTabFileRepository(),
            soundFontRepository = FakeSoundFontRepository(),
            dispatchers = AppDispatchers(
                io = mainDispatcherRule.dispatcher,
                default = mainDispatcherRule.dispatcher,
                main = mainDispatcherRule.dispatcher
            )
        )
    }
}
