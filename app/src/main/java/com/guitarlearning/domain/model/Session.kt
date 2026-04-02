package com.guitarlearning.domain.model

import java.util.Date
import androidx.compose.runtime.Immutable

@Immutable
data class Session(
    val id: Int = 0,
    val startTime: Date,
    val endTime: Date,
    val duration: Long,
    val practicedTabs: List<PracticedTab> = emptyList()
)

@Immutable
data class PracticedTab(
    val tabId: String,
    val tabName: String,
    val duration: Long
)
