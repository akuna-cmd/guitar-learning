package com.guitarlearning.presentation.main

import com.guitarlearning.domain.model.Session
import com.guitarlearning.testutil.FakeSessionRepository
import com.guitarlearning.testutil.MainDispatcherRule
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PracticeHeatmapViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun groupsSessionsByDayAndRoundsMinutesUp() = runTest {
        val repository = FakeSessionRepository()
        val viewModel = PracticeHeatmapViewModel(repository)
        val collectionJob = backgroundScope.launch(Dispatchers.Main) {
            viewModel.uiState.collect {}
        }

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val sameDayLater = (today.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 20)
        }
        val yesterday = (today.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, -1)
            set(Calendar.HOUR_OF_DAY, 14)
        }

        repository.sessionsFlow.value = listOf(
            Session(
                startTime = today.time,
                endTime = Date(today.timeInMillis + 30_000L),
                duration = 30_000L
            ),
            Session(
                startTime = sameDayLater.time,
                endTime = Date(sameDayLater.timeInMillis + 61_000L),
                duration = 61_000L
            ),
            Session(
                startTime = yesterday.time,
                endTime = Date(yesterday.timeInMillis + 10_000L),
                duration = 10_000L
            )
        )

        advanceUntilIdle()

        val activityData = viewModel.uiState.value.activityData
        assertEquals(2, activityData[startOfDay(today.time)])
        assertEquals(1, activityData[startOfDay(yesterday.time)])

        collectionJob.cancel()
    }

    private fun startOfDay(date: Date): Date {
        return Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }
}
