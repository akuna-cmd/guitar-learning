package com.guitarlearning.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface SessionDao {

    @Transaction
    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<SessionWithPracticedTabs>>

    @Transaction
    @Query("SELECT * FROM sessions WHERE startTime >= :since")
    fun getSessionsSince(since: Date): Flow<List<SessionWithPracticedTabs>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPracticedTabs(items: List<PracticedTabEntity>)

    @Query("DELETE FROM sessions")
    suspend fun clearHistory()

    @Transaction
    suspend fun insertSessionWithTabs(session: SessionEntity, tabs: List<PracticedTabEntity>) {
        val sessionId = insertSession(session).toInt()
        if (tabs.isNotEmpty()) {
            insertPracticedTabs(
                tabs.map { it.copy(sessionId = sessionId) }
            )
        }
    }
}
