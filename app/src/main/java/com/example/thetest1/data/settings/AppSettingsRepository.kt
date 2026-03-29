package com.example.thetest1.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.thetest1.presentation.main.FretboardDisplayMode
import com.example.thetest1.presentation.main.TabDisplayMode
import com.example.thetest1.presentation.main.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class AppSettingsSnapshot(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val normalSpeed: Float = 1.0f,
    val practiceSpeed: Float = 0.25f,
    val normalTabScale: Float = 1.0f,
    val practiceTabScale: Float = 1.0f,
    val tabDisplayMode: TabDisplayMode = TabDisplayMode.TAB_AND_NOTES,
    val fretboardDisplayMode: FretboardDisplayMode = FretboardDisplayMode.DETAILED,
    val updatedAt: Long = 0L
)

class AppSettingsRepository(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val normalSpeed = floatPreferencesKey("normal_speed")
        val practiceSpeed = floatPreferencesKey("practice_speed")
        val normalTabScale = floatPreferencesKey("normal_tab_scale")
        val practiceTabScale = floatPreferencesKey("practice_tab_scale")
        val tabDisplayMode = stringPreferencesKey("tab_display_mode")
        val fretboardDisplayMode = stringPreferencesKey("fretboard_display_mode")
        val settingsUpdatedAt = longPreferencesKey("settings_updated_at")
        val syncOwnerUid = stringPreferencesKey("cloud_sync_owner_uid")
        val lastCloudSyncAt = longPreferencesKey("last_cloud_sync_at")
    }

    fun observeSettings(): Flow<AppSettingsSnapshot> {
        return dataStore.data.map(::preferencesToSnapshot)
    }

    suspend fun getSettings(): AppSettingsSnapshot {
        return preferencesToSnapshot(dataStore.data.first())
    }

    fun observeLastCloudSyncAt(): Flow<Long?> {
        return dataStore.data.map { it[Keys.lastCloudSyncAt] }
    }

    suspend fun getSyncOwnerUid(): String? {
        return dataStore.data.first()[Keys.syncOwnerUid]
    }

    suspend fun setSyncOwnerUid(uid: String?) {
        dataStore.edit { preferences ->
            if (uid.isNullOrBlank()) {
                preferences.remove(Keys.syncOwnerUid)
            } else {
                preferences[Keys.syncOwnerUid] = uid
            }
        }
    }

    suspend fun setLastCloudSyncAt(timestamp: Long?) {
        dataStore.edit { preferences ->
            if (timestamp == null) {
                preferences.remove(Keys.lastCloudSyncAt)
            } else {
                preferences[Keys.lastCloudSyncAt] = timestamp
            }
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) = updateSettings { it.copy(themeMode = mode) }

    suspend fun setNormalSpeed(speed: Float) = updateSettings { it.copy(normalSpeed = speed) }

    suspend fun setPracticeSpeed(speed: Float) = updateSettings { it.copy(practiceSpeed = speed) }

    suspend fun setNormalTabScale(scale: Float) = updateSettings { it.copy(normalTabScale = scale) }

    suspend fun setPracticeTabScale(scale: Float) = updateSettings { it.copy(practiceTabScale = scale) }

    suspend fun setTabDisplayMode(mode: TabDisplayMode) = updateSettings { it.copy(tabDisplayMode = mode) }

    suspend fun setFretboardDisplayMode(mode: FretboardDisplayMode) =
        updateSettings { it.copy(fretboardDisplayMode = mode) }

    suspend fun replaceSettings(snapshot: AppSettingsSnapshot) {
        dataStore.edit { preferences ->
            writeSnapshot(preferences, snapshot)
        }
    }

    suspend fun resetSettingsToDefaults() {
        replaceSettings(AppSettingsSnapshot(updatedAt = System.currentTimeMillis()))
    }

    private suspend fun updateSettings(transform: (AppSettingsSnapshot) -> AppSettingsSnapshot) {
        dataStore.edit { preferences ->
            val current = preferencesToSnapshot(preferences)
            writeSnapshot(
                preferences,
                transform(current).copy(updatedAt = System.currentTimeMillis())
            )
        }
    }

    private fun preferencesToSnapshot(preferences: Preferences): AppSettingsSnapshot {
        return AppSettingsSnapshot(
            themeMode = ThemeMode.valueOf(preferences[Keys.themeMode] ?: ThemeMode.SYSTEM.name),
            normalSpeed = preferences[Keys.normalSpeed] ?: 1.0f,
            practiceSpeed = preferences[Keys.practiceSpeed] ?: 0.25f,
            normalTabScale = preferences[Keys.normalTabScale] ?: 1.0f,
            practiceTabScale = preferences[Keys.practiceTabScale] ?: 1.0f,
            tabDisplayMode = TabDisplayMode.valueOf(
                preferences[Keys.tabDisplayMode] ?: TabDisplayMode.TAB_AND_NOTES.name
            ),
            fretboardDisplayMode = FretboardDisplayMode.valueOf(
                preferences[Keys.fretboardDisplayMode] ?: FretboardDisplayMode.DETAILED.name
            ),
            updatedAt = preferences[Keys.settingsUpdatedAt] ?: 0L
        )
    }

    private fun writeSnapshot(preferences: MutablePreferences, snapshot: AppSettingsSnapshot) {
        preferences[Keys.themeMode] = snapshot.themeMode.name
        preferences[Keys.normalSpeed] = snapshot.normalSpeed
        preferences[Keys.practiceSpeed] = snapshot.practiceSpeed
        preferences[Keys.normalTabScale] = snapshot.normalTabScale
        preferences[Keys.practiceTabScale] = snapshot.practiceTabScale
        preferences[Keys.tabDisplayMode] = snapshot.tabDisplayMode.name
        preferences[Keys.fretboardDisplayMode] = snapshot.fretboardDisplayMode.name
        preferences[Keys.settingsUpdatedAt] = snapshot.updatedAt
    }
}
