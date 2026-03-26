package com.example.thetest1.domain.usecase

import com.example.thetest1.domain.repository.TabRepository

class MarkTabOfflineReadyUseCase(
    private val tabRepository: TabRepository
) {
    suspend operator fun invoke(tabId: String, ready: Boolean) {
        tabRepository.markOfflineReady(tabId, ready)
    }
}
