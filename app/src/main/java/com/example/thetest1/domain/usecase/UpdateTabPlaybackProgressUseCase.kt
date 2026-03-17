package com.example.thetest1.domain.usecase

import com.example.thetest1.domain.model.TabPlaybackProgress
import com.example.thetest1.domain.repository.TabPlaybackProgressRepository

class UpdateTabPlaybackProgressUseCase(
    private val repository: TabPlaybackProgressRepository
) {
    suspend operator fun invoke(progress: TabPlaybackProgress) {
        repository.upsert(progress)
    }
}
