package com.example.thetest1.domain.repository

import kotlinx.coroutines.flow.Flow

interface SyncRepository {
    fun isSyncing(): Flow<Boolean>
    suspend fun syncData(): Result<Unit>
    suspend fun deleteUserTab(tab: com.example.thetest1.domain.model.TabItem): Result<Unit>
    suspend fun clearRemoteData(): Result<Unit>
    suspend fun clearLocalUserData(): Result<Unit>
    fun getLastSyncedTime(): Flow<Long?>
}
