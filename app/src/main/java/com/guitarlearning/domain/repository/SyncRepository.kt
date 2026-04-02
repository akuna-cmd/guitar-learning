package com.guitarlearning.domain.repository

import kotlinx.coroutines.flow.Flow

interface SyncRepository {
    fun isSyncing(): Flow<Boolean>
    suspend fun syncData(): Result<Unit>
    suspend fun deleteUserTab(tab: com.guitarlearning.domain.model.TabItem): Result<Unit>
    suspend fun clearRemoteData(): Result<Unit>
    suspend fun clearLocalUserData(): Result<Unit>
    fun getLastSyncedTime(): Flow<Long?>
}
