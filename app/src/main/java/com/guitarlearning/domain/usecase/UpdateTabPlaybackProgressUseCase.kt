package com.guitarlearning.domain.usecase

import com.guitarlearning.domain.model.TabPlaybackProgress
import com.guitarlearning.domain.repository.TabPlaybackProgressRepository

class UpdateTabPlaybackProgressUseCase(
    private val repository: TabPlaybackProgressRepository
) {
    suspend operator fun invoke(progress: TabPlaybackProgress) {
        repository.upsert(progress)
    }
}
