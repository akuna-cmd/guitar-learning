package com.example.thetest1.domain.repository

import com.example.thetest1.domain.model.TabPlaybackProgress
import kotlinx.coroutines.flow.Flow

interface TabPlaybackProgressRepository {
    fun observeAll(): Flow<List<TabPlaybackProgress>>
    suspend fun getByTabId(tabId: String): TabPlaybackProgress?
    suspend fun upsert(progress: TabPlaybackProgress)
}
