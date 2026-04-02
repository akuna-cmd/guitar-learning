package com.guitarlearning.data.remote

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiAssistantConfigProvider @Inject constructor(
    private val remoteConfig: FirebaseRemoteConfig
) {
    @Volatile
    private var initialized = false

    suspend fun prefetch() {
        if (initialized) return
        runCatching {
            remoteConfig.fetchAndActivate().await()
        }
        initialized = true
    }

    suspend fun getModelName(): String {
        prefetch()
        return remoteConfig.getString(AiAssistantConfig.RemoteModelKey)
            .ifBlank { AiAssistantConfig.DefaultModelName }
    }
}
