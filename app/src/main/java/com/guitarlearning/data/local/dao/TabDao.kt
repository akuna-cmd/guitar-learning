package com.guitarlearning.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.guitarlearning.data.local.entity.TabTagCrossRef
import com.guitarlearning.data.local.entity.TabEntity
import com.guitarlearning.data.local.entity.TabWithTags
import com.guitarlearning.data.local.entity.TagEntity
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

    @Transaction
    @Query("SELECT * FROM tabs WHERE isUserTab = 0")
    fun getTabs(): Flow<List<TabWithTags>>

    @Transaction
    @Query("SELECT * FROM tabs WHERE isUserTab = 1")
    fun getUserTabs(): Flow<List<TabWithTags>>

    @Query("SELECT COUNT(*) FROM tabs WHERE isUserTab = 1")
    fun getUserTabsCount(): Flow<Int>

    @Transaction
    @Query("SELECT * FROM tabs WHERE id = :id")
    suspend fun getTabById(id: String): TabWithTags?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTags(tags: List<TagEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTabTagCrossRefs(crossRefs: List<TabTagCrossRef>)

    @Query("DELETE FROM tab_tag_cross_ref WHERE tabId = :tabId")
    suspend fun deleteTagCrossRefsByTabId(tabId: String)

    @Transaction
    suspend fun replaceTags(tabId: String, tags: List<String>) {
        deleteTagCrossRefsByTabId(tabId)
        if (tags.isEmpty()) return
        insertTags(tags.map(::TagEntity))
        insertTabTagCrossRefs(tags.map { tag -> TabTagCrossRef(tabId = tabId, tagName = tag) })
    }
}
