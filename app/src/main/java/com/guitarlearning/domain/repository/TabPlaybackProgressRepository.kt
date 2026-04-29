package com.guitarlearning.domain.repository

import com.guitarlearning.domain.model.TabPlaybackProgress
import kotlinx.coroutines.flow.Flow

interface TabPlaybackProgressRepository {
    fun observeAll(): Flow<List<TabPlaybackProgress>>
    suspend fun getByTabId(tabId: String): TabPlaybackProgress?
    suspend fun upsert(progress: TabPlaybackProgress)
    suspend fun removeByTabId(tabId: String)
    suspend fun replaceAll(progressList: List<TabPlaybackProgress>)
    suspend fun clearAll()
}
