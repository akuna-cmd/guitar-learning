package com.example.thetest1.domain.usecase

import com.example.thetest1.domain.model.TabPlaybackProgress
import com.example.thetest1.domain.repository.TabPlaybackProgressRepository
import kotlinx.coroutines.flow.Flow

class ObserveTabPlaybackProgressUseCase(
    private val repository: TabPlaybackProgressRepository
) {
    operator fun invoke(): Flow<List<TabPlaybackProgress>> {
        return repository.observeAll()
    }
}
