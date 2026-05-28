package com.guitarlearning.data.session

import android.content.Context
import android.net.Uri
import com.guitarlearning.core.session.SessionHistoryTransfer
import com.guitarlearning.domain.model.PracticedTab
import com.guitarlearning.domain.model.Session
import com.guitarlearning.domain.repository.SessionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStreamReader
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionHistoryTransferImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionRepository: SessionRepository
) : SessionHistoryTransfer {

    override suspend fun exportHistory(target: Uri) {
        val payload = JSONArray().apply {
            sessionRepository.getAllSessionsSync().forEach { session ->
                put(session.toJson())
            }
        }
        context.contentResolver.openOutputStream(target)?.use { outputStream ->
            outputStream.write(payload.toString(2).toByteArray())
        }
    }

    override suspend fun importHistory(source: Uri) {
        val content = context.contentResolver.openInputStream(source)?.use { inputStream ->
            InputStreamReader(inputStream).readText()
        }.orEmpty()
        sessionRepository.importHistory(JSONArray(content).toSessions())
    }
}

private fun Session.toJson(): JSONObject =
    JSONObject().apply {
        put("id", id)
        put("startTime", startTime.time)
        put("endTime", endTime.time)
        put("duration", duration)
        put(
            "practicedTabs",
            JSONArray().apply {
                practicedTabs.forEach { tab ->
                    put(
                        JSONObject().apply {
                            put("tabId", tab.tabId)
                            put("tabName", tab.tabName)
                            put("duration", tab.duration)
                        }
                    )
                }
            }
        )
    }

private fun JSONArray.toSessions(): List<Session> =
    buildList {
        for (i in 0 until length()) {
            val session = getJSONObject(i)
            add(
                Session(
                    id = session.optInt("id", 0),
                    startTime = Date(session.getLong("startTime")),
                    endTime = Date(session.getLong("endTime")),
                    duration = session.getLong("duration"),
                    practicedTabs = session.optJSONArray("practicedTabs").toPracticedTabs()
                )
            )
        }
    }

private fun JSONArray?.toPracticedTabs(): List<PracticedTab> =
    buildList {
        if (this@toPracticedTabs == null) return@buildList
        for (i in 0 until length()) {
            val tab = getJSONObject(i)
            add(
                PracticedTab(
                    tabId = tab.getString("tabId"),
                    tabName = tab.getString("tabName"),
                    duration = tab.getLong("duration")
                )
            )
        }
    }
