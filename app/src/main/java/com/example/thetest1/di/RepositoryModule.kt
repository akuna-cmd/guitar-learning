package com.example.thetest1.di

import com.example.thetest1.data.repository.AiAssistantRepositoryImpl
import com.example.thetest1.data.repository.AudioNoteRepositoryImpl
import com.example.thetest1.data.repository.FirestoreSyncRepositoryImpl
import com.example.thetest1.data.repository.GoalRepositoryImpl
import com.example.thetest1.data.repository.SessionRepositoryImpl
import com.example.thetest1.data.repository.SoundFontRepositoryImpl
import com.example.thetest1.data.repository.TabFileRepositoryImpl
import com.example.thetest1.data.repository.TabPlaybackProgressRepositoryImpl
import com.example.thetest1.data.repository.TabRepositoryImpl
import com.example.thetest1.data.repository.TextNoteRepositoryImpl
import com.example.thetest1.domain.repository.AiAssistantRepository
import com.example.thetest1.domain.repository.AudioNoteRepository
import com.example.thetest1.domain.repository.GoalRepository
import com.example.thetest1.domain.repository.SessionRepository
import com.example.thetest1.domain.repository.SoundFontRepository
import com.example.thetest1.domain.repository.SyncRepository
import com.example.thetest1.domain.repository.TabFileRepository
import com.example.thetest1.domain.repository.TabPlaybackProgressRepository
import com.example.thetest1.domain.repository.TabRepository
import com.example.thetest1.domain.repository.TextNoteRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTabRepository(
        implementation: TabRepositoryImpl
    ): TabRepository

    @Binds
    @Singleton
    abstract fun bindTabFileRepository(
        implementation: TabFileRepositoryImpl
    ): TabFileRepository

    @Binds
    @Singleton
    abstract fun bindSoundFontRepository(
        implementation: SoundFontRepositoryImpl
    ): SoundFontRepository

    @Binds
    @Singleton
    abstract fun bindAiAssistantRepository(
        implementation: AiAssistantRepositoryImpl
    ): AiAssistantRepository

    @Binds
    @Singleton
    abstract fun bindAudioNoteRepository(
        implementation: AudioNoteRepositoryImpl
    ): AudioNoteRepository

    @Binds
    @Singleton
    abstract fun bindTextNoteRepository(
        implementation: TextNoteRepositoryImpl
    ): TextNoteRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(
        implementation: SessionRepositoryImpl
    ): SessionRepository

    @Binds
    @Singleton
    abstract fun bindGoalRepository(
        implementation: GoalRepositoryImpl
    ): GoalRepository

    @Binds
    @Singleton
    abstract fun bindTabPlaybackProgressRepository(
        implementation: TabPlaybackProgressRepositoryImpl
    ): TabPlaybackProgressRepository

    @Binds
    @Singleton
    abstract fun bindSyncRepository(
        implementation: FirestoreSyncRepositoryImpl
    ): SyncRepository
}
