package com.example.thetest1.domain.repository

import android.net.Uri
import com.example.thetest1.data.model.Lesson
import com.example.thetest1.domain.model.TabItem
import kotlinx.coroutines.flow.Flow

interface TabRepository {
    fun getTabs(): Flow<List<TabItem>>
    suspend fun getLesson(id: String): Lesson?
    suspend fun updateTab(tab: TabItem)
    fun getCompletedLessonsCount(): Flow<Int>
    fun getTotalLessonsCount(): Flow<Int>
    suspend fun addUserTab(uri: Uri)
    suspend fun getUserTabs(): List<TabItem>
    fun getUserTabsCount(): Flow<Int>
    suspend fun deleteUserTab(tab: TabItem)
    suspend fun renameUserTab(tab: TabItem, newName: String)
    suspend fun getTabById(id: String): TabItem?
}
