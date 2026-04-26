package com.guitarlearning.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.guitarlearning.presentation.main.AiProvider
import com.guitarlearning.presentation.main.AppLanguage
import com.guitarlearning.presentation.main.FretboardDisplayMode
import com.guitarlearning.presentation.main.TabDisplayMode
import com.guitarlearning.presentation.main.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class AppSettingsSnapshot(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appLanguage: AppLanguage = AppLanguage.UKRAINIAN,
    val aiProvider: AiProvider = AiProvider.GEMINI,
    val localAiServerUrl: String = "",
    val normalSpeed: Float = 1.0f,
    val practiceSpeed: Float = 0.25f,
    val normalTabScale: Float = 1.0f,
    val practiceTabScale: Float = 1.0f,
    val tabDisplayMode: TabDisplayMode = TabDisplayMode.TAB_AND_NOTES,
    val fretboardDisplayMode: FretboardDisplayMode = FretboardDisplayMode.DETAILED,
    val updatedAt: Long = 0L
)

@Singleton
class AppSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val appLanguage = stringPreferencesKey("app_language")
        val aiProvider = stringPreferencesKey("ai_provider")
        val localAiServerUrl = stringPreferencesKey("local_ai_server_url")
        val normalSpeed = floatPreferencesKey("normal_speed")
        val practiceSpeed = floatPreferencesKey("practice_speed")
        val normalTabScale = floatPreferencesKey("normal_tab_scale")
        val practiceTabScale = floatPreferencesKey("practice_tab_scale")
        val tabDisplayMode = stringPreferencesKey("tab_display_mode")
        val fretboardDisplayMode = stringPreferencesKey("fretboard_display_mode")
        val settingsUpdatedAt = longPreferencesKey("settings_updated_at")
        val syncOwnerUid = stringPreferencesKey("cloud_sync_owner_uid")
        val lastCloudSyncAt = longPreferencesKey("last_cloud_sync_at")
        val pendingDeletedUserTabIds = stringSetPreferencesKey("pending_deleted_user_tab_ids")
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

    suspend fun getPendingDeletedUserTabIds(): Set<String> {
        return dataStore.data.first()[Keys.pendingDeletedUserTabIds].orEmpty()
    }

    suspend fun markUserTabPendingDeletion(tabId: String) {
        if (tabId.isBlank()) return
        dataStore.edit { preferences ->
            val current = preferences[Keys.pendingDeletedUserTabIds].orEmpty()
            preferences[Keys.pendingDeletedUserTabIds] = current + tabId
        }
    }

    suspend fun clearPendingDeletedUserTabIds(tabIds: Set<String>) {
        if (tabIds.isEmpty()) return
        dataStore.edit { preferences ->
            val current = preferences[Keys.pendingDeletedUserTabIds].orEmpty()
            val updated = current - tabIds
            if (updated.isEmpty()) {
                preferences.remove(Keys.pendingDeletedUserTabIds)
            } else {
                preferences[Keys.pendingDeletedUserTabIds] = updated
            }
        }
    }

    suspend fun clearAllPendingDeletedUserTabIds() {
        dataStore.edit { preferences ->
            preferences.remove(Keys.pendingDeletedUserTabIds)
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) = updateSettings { it.copy(themeMode = mode) }

    suspend fun setAppLanguage(language: AppLanguage) = updateSettings { it.copy(appLanguage = language) }

    suspend fun setAiProvider(provider: AiProvider) = updateSettings { it.copy(aiProvider = provider) }

    suspend fun setLocalAiServerUrl(url: String) = updateSettings { it.copy(localAiServerUrl = url) }

    suspend fun setAiSettings(provider: AiProvider, url: String) =
        updateSettings { it.copy(aiProvider = provider, localAiServerUrl = url) }

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
            appLanguage = AppLanguage.valueOf(preferences[Keys.appLanguage] ?: AppLanguage.UKRAINIAN.name),
            aiProvider = AiProvider.valueOf(preferences[Keys.aiProvider] ?: AiProvider.GEMINI.name),
            localAiServerUrl = preferences[Keys.localAiServerUrl].orEmpty(),
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
        preferences[Keys.appLanguage] = snapshot.appLanguage.name
        preferences[Keys.aiProvider] = snapshot.aiProvider.name
        preferences[Keys.localAiServerUrl] = snapshot.localAiServerUrl
        preferences[Keys.normalSpeed] = snapshot.normalSpeed
        preferences[Keys.practiceSpeed] = snapshot.practiceSpeed
        preferences[Keys.normalTabScale] = snapshot.normalTabScale
        preferences[Keys.practiceTabScale] = snapshot.practiceTabScale
        preferences[Keys.tabDisplayMode] = snapshot.tabDisplayMode.name
        preferences[Keys.fretboardDisplayMode] = snapshot.fretboardDisplayMode.name
        preferences[Keys.settingsUpdatedAt] = snapshot.updatedAt
    }
}
