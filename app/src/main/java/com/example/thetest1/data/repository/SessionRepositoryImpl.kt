package com.example.thetest1.data.repository

import com.example.thetest1.data.local.PracticedTabEntity
import com.example.thetest1.data.local.SessionEntity
import com.example.thetest1.data.local.SessionWithPracticedTabs
import com.example.thetest1.data.local.SessionDao
import com.example.thetest1.domain.model.PracticedTab
import com.example.thetest1.domain.model.Session
import com.example.thetest1.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Date

class SessionRepositoryImpl(
    private val sessionDao: SessionDao
) : SessionRepository {

    override fun getAllSessions(): Flow<List<Session>> {
        return sessionDao.getAllSessions().map { it.toDomain() }
    }

    override suspend fun getAllSessionsSync(): List<Session> {
        return sessionDao.getAllSessions().first().toDomain()
    }

    override fun getSessionsSince(since: Date): Flow<List<Session>> {
        return sessionDao.getSessionsSince(since).map { it.toDomain() }
    }

    override suspend fun addSession(session: Session) {
        sessionDao.insertSessionWithTabs(
            session = SessionEntity(
                id = session.id,
                startTime = session.startTime,
                endTime = session.endTime,
                duration = session.duration
            ),
            tabs = session.practicedTabs.map {
                PracticedTabEntity(
                    sessionId = 0,
                    tabId = it.tabId,
                    tabName = it.tabName,
                    duration = it.duration
                )
            }
        )
    }

    override suspend fun importHistory(sessions: List<Session>) {
        if (sessions.isEmpty()) return

        val existingSessions = getAllSessionsSync()
        val existingIds = existingSessions.mapTo(mutableSetOf()) { it.id }.apply { remove(0) }
        val existingKeys = existingSessions.mapTo(mutableSetOf()) { it.syncKey() }

        sessions.forEach { session ->
            val sessionIdExists = session.id != 0 && session.id in existingIds
            val sessionKey = session.syncKey()
            if (sessionIdExists || sessionKey in existingKeys) return@forEach

            addSession(session)
            if (session.id != 0) {
                existingIds.add(session.id)
            }
            existingKeys.add(sessionKey)
        }
    }

    override suspend fun clearHistory() {
        sessionDao.clearHistory()
    }
}

private fun List<SessionWithPracticedTabs>.toDomain(): List<Session> =
    map { relation ->
        Session(
            id = relation.session.id,
            startTime = relation.session.startTime,
            endTime = relation.session.endTime,
            duration = relation.session.duration,
            practicedTabs = relation.practicedTabs.map {
                PracticedTab(
                    tabId = it.tabId,
                    tabName = it.tabName,
                    duration = it.duration
                )
            }
        )
    }

private fun Session.syncKey(): String =
    buildString {
        append(startTime.time)
        append('|')
        append(endTime.time)
        append('|')
        append(duration)
        append('|')
        append(
            practicedTabs.joinToString(";") {
                "${it.tabId}:${it.tabName}:${it.duration}"
            }
        )
    }
