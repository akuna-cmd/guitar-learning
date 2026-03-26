package com.example.thetest1.domain.repository

import com.example.thetest1.domain.model.Lesson
import com.example.thetest1.domain.model.TabItem
import kotlinx.coroutines.flow.Flow

interface TabRepository {
    fun getTabs(): Flow<List<TabItem>>
    suspend fun getLesson(id: String): Lesson?
    suspend fun updateTab(tab: TabItem)
    fun getCompletedLessonsCount(): Flow<Int>
    fun getTotalLessonsCount(): Flow<Int>
    suspend fun addUserTab(uriString: String)
    suspend fun getUserTabs(): List<TabItem>
    fun getUserTabsCount(): Flow<Int>
    suspend fun deleteUserTab(tab: TabItem)
    suspend fun renameUserTab(tab: TabItem, newName: String)
    suspend fun getTabById(id: String): TabItem?
    suspend fun markTabOpened(tabId: String, openedAt: Long = System.currentTimeMillis())
    suspend fun updateTabTags(tabId: String, tags: List<String>)
    suspend fun updateTabFolder(tabId: String, folder: String)
    suspend fun markOfflineReady(tabId: String, ready: Boolean)
}
