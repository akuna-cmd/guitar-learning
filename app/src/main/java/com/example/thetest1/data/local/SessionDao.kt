package com.example.thetest1.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.thetest1.domain.model.Session
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface SessionDao {

    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<Session>>

    @Query("SELECT * FROM sessions WHERE startTime >= :since")
    fun getSessionsSince(since: Date): Flow<List<Session>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: Session)
}
