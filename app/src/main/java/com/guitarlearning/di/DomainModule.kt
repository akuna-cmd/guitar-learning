package com.guitarlearning.di

import com.guitarlearning.domain.repository.GoalRepository
import com.guitarlearning.domain.repository.SessionRepository
import com.guitarlearning.domain.repository.TabRepository
import com.guitarlearning.domain.usecase.ObserveGoalsProgressUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DomainModule {

    @Provides
    @Singleton
    fun provideObserveGoalsProgressUseCase(
        goalRepository: GoalRepository,
        tabRepository: TabRepository,
        sessionRepository: SessionRepository
    ): ObserveGoalsProgressUseCase {
        return ObserveGoalsProgressUseCase(
            goalRepository = goalRepository,
            tabRepository = tabRepository,
            sessionRepository = sessionRepository
        )
    }
}
