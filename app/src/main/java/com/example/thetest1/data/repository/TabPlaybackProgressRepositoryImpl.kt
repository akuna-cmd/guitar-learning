package com.example.thetest1.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.thetest1.domain.model.TabPlaybackProgress
import com.example.thetest1.domain.repository.TabPlaybackProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

class TabPlaybackProgressRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : TabPlaybackProgressRepository {

    private val progressKey = stringPreferencesKey("tab_playback_progress")

    override fun observeAll(): Flow<List<TabPlaybackProgress>> {
        return dataStore.data.map { preferences ->
            val raw = preferences[progressKey].orEmpty()
            parseAll(raw)
        }
    }

    override suspend fun getByTabId(tabId: String): TabPlaybackProgress? {
        val raw = dataStore.data.first()[progressKey].orEmpty()
        return parseOne(raw, tabId)
    }

    override suspend fun upsert(progress: TabPlaybackProgress) {
        dataStore.edit { preferences ->
            val raw = preferences[progressKey].orEmpty()
            val json = if (raw.isBlank()) JSONObject() else JSONObject(raw)
            json.put(progress.tabId, JSONObject().apply {
                put("tabId", progress.tabId)
                put("tabName", progress.tabName)
                put("lastTick", progress.lastTick)
                put("lastBarIndex", progress.lastBarIndex)
                put("totalBars", progress.totalBars)
                put("updatedAt", progress.updatedAt)
            })
            preferences[progressKey] = json.toString()
        }
    }

    private fun parseOne(raw: String, tabId: String): TabPlaybackProgress? {
        if (raw.isBlank()) return null
        val json = JSONObject(raw)
        if (!json.has(tabId)) return null
        return jsonToProgress(json.getJSONObject(tabId))
    }

    private fun parseAll(raw: String): List<TabPlaybackProgress> {
        if (raw.isBlank()) return emptyList()
        val json = JSONObject(raw)
        val keys = json.keys()
        val list = mutableListOf<TabPlaybackProgress>()
        while (keys.hasNext()) {
            val key = keys.next()
            val obj = json.optJSONObject(key) ?: continue
            list.add(jsonToProgress(obj))
        }
        return list
    }

    private fun jsonToProgress(obj: JSONObject): TabPlaybackProgress {
        return TabPlaybackProgress(
            tabId = obj.optString("tabId"),
            tabName = obj.optString("tabName"),
            lastTick = obj.optLong("lastTick"),
            lastBarIndex = obj.optInt("lastBarIndex"),
            totalBars = obj.optInt("totalBars"),
            updatedAt = obj.optLong("updatedAt")
        )
    }
}
