package com.guitarlearning.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guitarlearning.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

data class PracticeHeatmapUiState(
    val activityData: Map<Date, Int> = emptyMap()
)

@HiltViewModel
class PracticeHeatmapViewModel @Inject constructor(
    sessionRepository: SessionRepository
) : ViewModel() {

    private val since: Date = Calendar.getInstance().apply {
        add(Calendar.MONTH, -1)
    }.time

    val uiState: StateFlow<PracticeHeatmapUiState> = sessionRepository.getSessionsSince(since)
        .map { sessions ->
            val activityData = sessions
                .groupBy { getStartOfDay(it.startTime) }
                .mapValues { (_, sessions) ->
                    val totalMs = sessions.sumOf { it.duration }
                    if (totalMs <= 0L) 0 else ((totalMs + 59_999L) / 60_000L).toInt()
                } // minutes, rounded up so short sessions are still visible
            PracticeHeatmapUiState(activityData)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PracticeHeatmapUiState()
        )

    private fun getStartOfDay(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }
}
