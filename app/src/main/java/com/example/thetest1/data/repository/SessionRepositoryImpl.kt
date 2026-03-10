package com.example.thetest1.data.repository

import com.example.thetest1.data.local.SessionDao
import com.example.thetest1.domain.model.Session
import com.example.thetest1.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import java.util.Date

class SessionRepositoryImpl(
    private val sessionDao: SessionDao
) : SessionRepository {

    override fun getAllSessions(): Flow<List<Session>> {
        return sessionDao.getAllSessions()
    }

    override fun getSessionsSince(since: Date): Flow<List<Session>> {
        return sessionDao.getSessionsSince(since)
    }

    override suspend fun addSession(session: Session) {
        sessionDao.insertSession(session)
    }
}
