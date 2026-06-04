package com.guitarlearning.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.guitarlearning.domain.settings.AiProvider
import com.guitarlearning.domain.settings.AppLanguage
import com.guitarlearning.domain.settings.AppSettingsSnapshot
import com.guitarlearning.domain.settings.TabDisplayMode
import com.guitarlearning.domain.settings.ThemeMode
import com.guitarlearning.domain.repository.AppSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreAppSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : AppSettingsRepository {
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
        val settingsUpdatedAt = longPreferencesKey("settings_updated_at")
        val syncOwnerUid = stringPreferencesKey("cloud_sync_owner_uid")
        val lastCloudSyncAt = longPreferencesKey("last_cloud_sync_at")
        val pendingDeletedUserTabIds = stringSetPreferencesKey("pending_deleted_user_tab_ids")
    }

    override fun observeSettings(): Flow<AppSettingsSnapshot> {
        return dataStore.data.map(::preferencesToSnapshot)
    }

    override suspend fun getSettings(): AppSettingsSnapshot {
        return preferencesToSnapshot(dataStore.data.first())
    }

    override fun observeLastCloudSyncAt(): Flow<Long?> {
        return dataStore.data.map { it[Keys.lastCloudSyncAt] }
    }

    override suspend fun getSyncOwnerUid(): String? {
        return dataStore.data.first()[Keys.syncOwnerUid]
    }

    override suspend fun setSyncOwnerUid(uid: String?) {
        dataStore.edit { preferences ->
            if (uid.isNullOrBlank()) {
                preferences.remove(Keys.syncOwnerUid)
            } else {
                preferences[Keys.syncOwnerUid] = uid
            }
        }
    }

    override suspend fun setLastCloudSyncAt(timestamp: Long?) {
        dataStore.edit { preferences ->
            if (timestamp == null) {
                preferences.remove(Keys.lastCloudSyncAt)
            } else {
                preferences[Keys.lastCloudSyncAt] = timestamp
            }
        }
    }

    override suspend fun getPendingDeletedUserTabIds(): Set<String> {
        return dataStore.data.first()[Keys.pendingDeletedUserTabIds].orEmpty()
    }

    override suspend fun markUserTabPendingDeletion(tabId: String) {
        if (tabId.isBlank()) return
        dataStore.edit { preferences ->
            val current = preferences[Keys.pendingDeletedUserTabIds].orEmpty()
            preferences[Keys.pendingDeletedUserTabIds] = current + tabId
        }
    }

    override suspend fun clearPendingDeletedUserTabIds(tabIds: Set<String>) {
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

    override suspend fun clearAllPendingDeletedUserTabIds() {
        dataStore.edit { preferences ->
            preferences.remove(Keys.pendingDeletedUserTabIds)
        }
    }

    override suspend fun setThemeMode(mode: ThemeMode) = updateSettings { it.copy(themeMode = mode) }

    override suspend fun setAppLanguage(language: AppLanguage) = updateSettings { it.copy(appLanguage = language) }

    override suspend fun setAiProvider(provider: AiProvider) = updateSettings { it.copy(aiProvider = provider) }

    override suspend fun setLocalAiServerUrl(url: String) = updateSettings { it.copy(localAiServerUrl = url) }

    override suspend fun setAiSettings(provider: AiProvider, url: String) =
        updateSettings { it.copy(aiProvider = provider, localAiServerUrl = url) }

    override suspend fun setNormalSpeed(speed: Float) = updateSettings { it.copy(normalSpeed = speed) }

    override suspend fun setPracticeSpeed(speed: Float) = updateSettings { it.copy(practiceSpeed = speed) }

    override suspend fun setNormalTabScale(scale: Float) = updateSettings { it.copy(normalTabScale = scale) }

    override suspend fun setPracticeTabScale(scale: Float) = updateSettings { it.copy(practiceTabScale = scale) }

    override suspend fun setTabDisplayMode(mode: TabDisplayMode) = updateSettings { it.copy(tabDisplayMode = mode) }

    override suspend fun replaceSettings(snapshot: AppSettingsSnapshot) {
        dataStore.edit { preferences ->
            writeSnapshot(preferences, snapshot)
        }
    }

    override suspend fun resetSettingsToDefaults() {
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
            practiceSpeed = preferences[Keys.practiceSpeed] ?: 0.3f,
            normalTabScale = preferences[Keys.normalTabScale] ?: 1.5f,
            practiceTabScale = preferences[Keys.practiceTabScale] ?: 1.5f,
            tabDisplayMode = TabDisplayMode.valueOf(
                preferences[Keys.tabDisplayMode] ?: TabDisplayMode.TAB_AND_NOTES.name
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
        preferences[Keys.settingsUpdatedAt] = snapshot.updatedAt
    }
}
