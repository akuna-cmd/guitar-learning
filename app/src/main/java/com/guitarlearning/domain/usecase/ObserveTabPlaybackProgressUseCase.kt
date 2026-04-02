package com.guitarlearning.domain.usecase

import com.guitarlearning.domain.model.TabPlaybackProgress
import com.guitarlearning.domain.repository.TabPlaybackProgressRepository
import kotlinx.coroutines.flow.Flow

class ObserveTabPlaybackProgressUseCase(
    private val repository: TabPlaybackProgressRepository
) {
    operator fun invoke(): Flow<List<TabPlaybackProgress>> {
        return repository.observeAll()
    }
}
