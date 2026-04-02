package com.guitarlearning.di

import android.content.Context
import com.guitarlearning.data.remote.AiAssistantConfigProvider
import com.guitarlearning.domain.repository.TabPlaybackProgressRepository
import com.guitarlearning.domain.repository.TabRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppWarmup @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tabRepository: TabRepository,
    private val progressRepository: TabPlaybackProgressRepository,
    private val auth: FirebaseAuth,
    private val aiAssistantConfigProvider: AiAssistantConfigProvider,
    private val dispatchers: AppDispatchers
) {
    suspend fun warm() {
        withContext(dispatchers.io) {
            runCatching { tabRepository.getTabs().first() }
            runCatching { tabRepository.getUserTabs().first() }
            runCatching { progressRepository.observeAll().first() }
            runCatching { auth.currentUser }
            runCatching { context.assets.open("tab_viewer.html").close() }
            runCatching { context.assets.open("alphatab_local.js").close() }
            runCatching { aiAssistantConfigProvider.prefetch() }
        }
    }
}
