package com.guitarlearning.domain.usecase

import com.guitarlearning.domain.model.TabPlaybackProgress
import com.guitarlearning.domain.repository.TabPlaybackProgressRepository

class GetTabPlaybackProgressUseCase(
    private val repository: TabPlaybackProgressRepository
) {
    suspend operator fun invoke(tabId: String): TabPlaybackProgress? {
        return repository.getByTabId(tabId)
    }
}
