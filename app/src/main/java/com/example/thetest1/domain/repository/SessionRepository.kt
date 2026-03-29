package com.example.thetest1.domain.repository

import com.example.thetest1.domain.model.Session
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface SessionRepository {
    fun getAllSessions(): Flow<List<Session>>
    suspend fun getAllSessionsSync(): List<Session>
    fun getSessionsSince(since: Date): Flow<List<Session>>
    suspend fun addSession(session: Session)
    suspend fun importHistory(sessions: List<Session>)
    suspend fun clearHistory()
}
