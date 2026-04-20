package com.guitarlearning.presentation.main
import com.guitarlearning.di.AppDispatchers
import com.guitarlearning.testutil.FakeSessionRepository
import com.guitarlearning.testutil.FakeTabPlaybackProgressRepository
import com.guitarlearning.testutil.FakeTabRepository
import com.guitarlearning.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun tracksPracticeAcrossTabSwitchesAndPersistsSessionSummary() = runTest {
        val sessionRepository = FakeSessionRepository()
        val viewModel = MainViewModel(
            sessionRepository = sessionRepository,
            tabRepository = FakeTabRepository(),
            progressRepository = FakeTabPlaybackProgressRepository(),
            dispatchers = AppDispatchers(
                io = mainDispatcherRule.dispatcher,
                default = mainDispatcherRule.dispatcher,
                main = mainDispatcherRule.dispatcher
            )
        )

        viewModel.startSession()
        viewModel.setActiveTab("tab-a", "Warmup")
        advanceTimeBy(2_100L)

        viewModel.setActiveTab("tab-b", "Solo")
        advanceTimeBy(1_100L)

        viewModel.stopSession()
        advanceUntilIdle()

        val savedSession = sessionRepository.addedSessions.single()
        assertEquals(2, savedSession.practicedTabs.size)
        assertEquals("tab-a", savedSession.practicedTabs[0].tabId)
        assertEquals(2_000L, savedSession.practicedTabs[0].duration)
        assertEquals("tab-b", savedSession.practicedTabs[1].tabId)
        assertEquals(1_000L, savedSession.practicedTabs[1].duration)
        assertFalse(viewModel.uiState.value.isSessionActive)
    }
}
