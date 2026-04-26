package com.guitarlearning.presentation.tab_viewer

import android.content.Context
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
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class TabViewerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun updatePlaybackProgress_overwritesHigherSavedBarWithCurrentLowerBar() = runTest {
        val progressRepository = FakeTabPlaybackProgressRepository(
            initialProgress = listOf(
                TabPlaybackProgress(
                    tabId = "song-1",
                    tabName = "Song",
                    lastTick = 600L,
                    lastBarIndex = 6,
                    totalBars = 10,
                    updatedAt = 1_000L
                )
            )
        )
        val viewModel = createViewModel(
            tabRepository = FakeTabRepository(initialTabs = listOf(testTabItem(id = "song-1"))),
            progressRepository = progressRepository
        )

        viewModel.loadLesson("song-1")
        advanceUntilIdle()
        viewModel.markRestoreCompleted()

        viewModel.updatePlaybackProgress(
            lessonId = "song-1",
            lessonTitle = "Song",
            tick = 200L,
            isPlaying = false,
            barIndex = 2,
            totalBars = 10
        )
        advanceUntilIdle()

        assertEquals(2, progressRepository.getByTabId("song-1")?.lastBarIndex)
        assertEquals(200L, progressRepository.getByTabId("song-1")?.lastTick)
    }

    @Test
    fun updatePlaybackProgress_overwritesCompletedProgressAfterRewind() = runTest {
        val progressRepository = FakeTabPlaybackProgressRepository()
        val viewModel = createViewModel(
            tabRepository = FakeTabRepository(initialTabs = listOf(testTabItem(id = "song-1"))),
            progressRepository = progressRepository
        )

        viewModel.loadLesson("song-1")
        advanceUntilIdle()

        viewModel.updatePlaybackProgress(
            lessonId = "song-1",
            lessonTitle = "Song",
            tick = 1_000L,
            isPlaying = true,
            barIndex = 10,
            totalBars = 10
        )
        advanceUntilIdle()

        viewModel.updatePlaybackProgress(
            lessonId = "song-1",
            lessonTitle = "Song",
            tick = 300L,
            isPlaying = false,
            barIndex = 3,
            totalBars = 10
        )
        advanceUntilIdle()

        assertEquals(3, progressRepository.getByTabId("song-1")?.lastBarIndex)
        assertEquals(300L, progressRepository.getByTabId("song-1")?.lastTick)
    }

    private fun createViewModel(
        tabRepository: FakeTabRepository,
        progressRepository: FakeTabPlaybackProgressRepository
    ): TabViewerViewModel {
        return TabViewerViewModel(
            context = RuntimeEnvironment.getApplication().applicationContext as Context,
            tabRepository = tabRepository,
            tabFileRepository = FakeTabFileRepository(),
            soundFontRepository = FakeSoundFontRepository(),
            tabPlaybackProgressRepository = progressRepository,
            dispatchers = AppDispatchers(
                io = mainDispatcherRule.dispatcher,
                default = mainDispatcherRule.dispatcher,
                main = mainDispatcherRule.dispatcher
            )
        )
    }
}
