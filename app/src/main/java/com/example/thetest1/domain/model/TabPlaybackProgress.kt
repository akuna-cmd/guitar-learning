package com.example.thetest1.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class TabPlaybackProgress(
    val tabId: String,
    val tabName: String,
    val lastTick: Long,
    val lastBarIndex: Int,
    val totalBars: Int,
    val updatedAt: Long
)
