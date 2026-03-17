package com.example.thetest1.domain.usecase

import com.example.thetest1.domain.model.TabPlaybackProgress
import com.example.thetest1.domain.repository.TabPlaybackProgressRepository

class GetTabPlaybackProgressUseCase(
    private val repository: TabPlaybackProgressRepository
) {
    suspend operator fun invoke(tabId: String): TabPlaybackProgress? {
        return repository.getByTabId(tabId)
    }
}
