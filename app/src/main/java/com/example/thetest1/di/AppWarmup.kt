package com.example.thetest1.di

import android.content.Context
import com.example.thetest1.data.remote.AiAssistantConfigProvider
import com.example.thetest1.domain.repository.TabPlaybackProgressRepository
import com.example.thetest1.domain.repository.TabRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppWarmup @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tabRepository: TabRepository,
    private val progressRepository: TabPlaybackProgressRepository,
    private val auth: FirebaseAuth,
    private val aiAssistantConfigProvider: AiAssistantConfigProvider
) {
    suspend fun warm() {
        runCatching { tabRepository.getTabs().first() }
        runCatching { tabRepository.getUserTabs() }
        runCatching { progressRepository.observeAll().first() }
        runCatching { auth.currentUser }
        runCatching { context.assets.open("tab_viewer.html").close() }
        runCatching { context.assets.open("alphatab_local.js").close() }
        runCatching { aiAssistantConfigProvider.prefetch() }
    }
}
