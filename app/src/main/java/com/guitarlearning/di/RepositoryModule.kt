package com.guitarlearning.di

import com.guitarlearning.data.repository.AiAssistantRepositoryImpl
import com.guitarlearning.data.repository.AudioNoteRepositoryImpl
import com.guitarlearning.data.repository.FirestoreSyncRepositoryImpl
import com.guitarlearning.data.repository.GoalRepositoryImpl
import com.guitarlearning.data.repository.SessionRepositoryImpl
import com.guitarlearning.data.repository.SoundFontRepositoryImpl
import com.guitarlearning.data.repository.TabFileRepositoryImpl
import com.guitarlearning.data.repository.TabPlaybackProgressRepositoryImpl
import com.guitarlearning.data.repository.TabRepositoryImpl
import com.guitarlearning.data.repository.TextNoteRepositoryImpl
import com.guitarlearning.domain.repository.AiAssistantRepository
import com.guitarlearning.domain.repository.AudioNoteRepository
import com.guitarlearning.domain.repository.GoalRepository
import com.guitarlearning.domain.repository.SessionRepository
import com.guitarlearning.domain.repository.SoundFontRepository
import com.guitarlearning.domain.repository.SyncRepository
import com.guitarlearning.domain.repository.TabFileRepository
import com.guitarlearning.domain.repository.TabPlaybackProgressRepository
import com.guitarlearning.domain.repository.TabRepository
import com.guitarlearning.domain.repository.TextNoteRepository
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
