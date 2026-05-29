package com.guitarlearning.domain.repository

import com.guitarlearning.core.preferences.AiProvider
import com.guitarlearning.core.preferences.AppLanguage
import com.guitarlearning.core.preferences.AppSettingsSnapshot
import com.guitarlearning.core.preferences.TabDisplayMode
import com.guitarlearning.core.preferences.ThemeMode
import kotlinx.coroutines.flow.Flow

interface AppSettingsRepository {
    fun observeSettings(): Flow<AppSettingsSnapshot>

    suspend fun getSettings(): AppSettingsSnapshot

    fun observeLastCloudSyncAt(): Flow<Long?>

    suspend fun getSyncOwnerUid(): String?

    suspend fun setSyncOwnerUid(uid: String?)

    suspend fun setLastCloudSyncAt(timestamp: Long?)

    suspend fun getPendingDeletedUserTabIds(): Set<String>

    suspend fun markUserTabPendingDeletion(tabId: String)

    suspend fun clearPendingDeletedUserTabIds(tabIds: Set<String>)

    suspend fun clearAllPendingDeletedUserTabIds()

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setAppLanguage(language: AppLanguage)

    suspend fun setAiProvider(provider: AiProvider)

    suspend fun setLocalAiServerUrl(url: String)

    suspend fun setAiSettings(provider: AiProvider, url: String)

    suspend fun setNormalSpeed(speed: Float)

    suspend fun setPracticeSpeed(speed: Float)

    suspend fun setNormalTabScale(scale: Float)

    suspend fun setPracticeTabScale(scale: Float)

    suspend fun setTabDisplayMode(mode: TabDisplayMode)

    suspend fun replaceSettings(snapshot: AppSettingsSnapshot)

    suspend fun resetSettingsToDefaults()
}
