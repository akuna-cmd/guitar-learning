package com.guitarlearning.data.repository

import com.guitarlearning.data.local.dao.SessionDao
import com.guitarlearning.data.local.entity.PracticedTabEntity
import com.guitarlearning.data.local.entity.SessionRow
import com.guitarlearning.data.local.entity.SessionEntity
import com.guitarlearning.domain.model.PracticedTab
import com.guitarlearning.domain.model.Session
import com.guitarlearning.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao
) : SessionRepository {

    override fun getAllSessions(): Flow<List<Session>> {
        return sessionDao.getAllSessionRows().map { it.toDomain() }
    }

    override suspend fun getAllSessionsSync(): List<Session> {
        return sessionDao.getAllSessionRows().first().toDomain()
    }

    override fun getSessionsSince(since: Date): Flow<List<Session>> {
        return sessionDao.getSessionRowsSince(since).map { it.toDomain() }
    }

    override suspend fun addSession(session: Session) {
        sessionDao.insertSessionWithTabs(
            session = SessionEntity(
                id = session.id,
                startTime = session.startTime,
                endTime = session.endTime
            ),
            tabs = session.practicedTabs.map {
                PracticedTabEntity(
                    sessionId = 0,
                    tabId = it.tabId,
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

private fun List<SessionRow>.toDomain(): List<Session> =
    groupBy(SessionRow::sessionId)
        .values
        .map { rows ->
            val first = rows.first()
            Session(
                id = first.sessionId,
                startTime = first.startTime,
                endTime = first.endTime,
                practicedTabs = rows.mapNotNull { row ->
                    val tabId = row.tabId ?: return@mapNotNull null
                    PracticedTab(
                        tabId = tabId,
                        tabName = row.tabName.orEmpty(),
                        duration = row.practicedDuration ?: 0L
                    )
                }
            )
        }
        .sortedByDescending { it.startTime.time }

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
