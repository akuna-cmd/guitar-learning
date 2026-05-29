package com.guitarlearning.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.guitarlearning.data.local.entity.TabEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TabDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTabs(tabs: List<TabEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTab(tab: TabEntity)

    @Update
    suspend fun updateTab(tab: TabEntity)

    @Delete
    suspend fun deleteTab(tab: TabEntity)

    @Query("DELETE FROM tabs")
    suspend fun deleteAllTabs()

    @Query("SELECT * FROM tabs WHERE isUserTab = 0")
    fun getTabs(): Flow<List<TabEntity>>

    @Query("SELECT * FROM tabs WHERE isUserTab = 1")
    fun getUserTabs(): Flow<List<TabEntity>>

    @Query("SELECT COUNT(*) FROM tabs WHERE isUserTab = 1")
    fun getUserTabsCount(): Flow<Int>

    @Query("SELECT * FROM tabs WHERE id = :id")
    suspend fun getTabById(id: String): TabEntity?
}
