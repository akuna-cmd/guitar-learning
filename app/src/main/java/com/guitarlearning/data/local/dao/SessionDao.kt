package com.guitarlearning.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.guitarlearning.data.local.entity.PracticedTabEntity
import com.guitarlearning.data.local.entity.SessionRow
import com.guitarlearning.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface SessionDao {

    @Query(
        """
        SELECT
            sessions.id AS sessionId,
            sessions.startTime AS startTime,
            sessions.endTime AS endTime,
            practiced_tabs.id AS practicedTabEntryId,
            practiced_tabs.tabId AS tabId,
            tabs.name AS tabName,
            practiced_tabs.duration AS practicedDuration
        FROM sessions
        LEFT JOIN practiced_tabs ON practiced_tabs.sessionId = sessions.id
        LEFT JOIN tabs ON tabs.id = practiced_tabs.tabId
        ORDER BY sessions.startTime DESC, practiced_tabs.id ASC
        """
    )
    fun getAllSessionRows(): Flow<List<SessionRow>>

    @Query(
        """
        SELECT
            sessions.id AS sessionId,
            sessions.startTime AS startTime,
            sessions.endTime AS endTime,
            practiced_tabs.id AS practicedTabEntryId,
            practiced_tabs.tabId AS tabId,
            tabs.name AS tabName,
            practiced_tabs.duration AS practicedDuration
        FROM sessions
        LEFT JOIN practiced_tabs ON practiced_tabs.sessionId = sessions.id
        LEFT JOIN tabs ON tabs.id = practiced_tabs.tabId
        WHERE sessions.startTime >= :since
        ORDER BY sessions.startTime DESC, practiced_tabs.id ASC
        """
    )
    fun getSessionRowsSince(since: Date): Flow<List<SessionRow>>

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
