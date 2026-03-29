package com.example.thetest1.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.thetest1.domain.model.TabItem
import kotlinx.coroutines.flow.Flow

@Dao
interface TabDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTabs(tabs: List<TabItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTab(tab: TabItem)

    @Update
    suspend fun updateTab(tab: TabItem)

    @Delete
    suspend fun deleteTab(tab: TabItem)

    @Query("DELETE FROM tabs")
    suspend fun deleteAllTabs()

    @Query("SELECT * FROM tabs WHERE isUserTab = 0")
    fun getTabs(): Flow<List<TabItem>>

    @Query("SELECT * FROM tabs WHERE isUserTab = 1")
    fun getUserTabs(): Flow<List<TabItem>>

    @Query("SELECT COUNT(*) FROM tabs WHERE isUserTab = 1")
    fun getUserTabsCount(): Flow<Int>

    @Query("SELECT * FROM tabs WHERE id = :id")
    suspend fun getTabById(id: String): TabItem?
}
