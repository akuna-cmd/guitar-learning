package com.example.thetest1.domain.model

data class TabPlaybackProgress(
    val tabId: String,
    val tabName: String,
    val lastTick: Long,
    val lastBarIndex: Int,
    val totalBars: Int,
    val updatedAt: Long
)
