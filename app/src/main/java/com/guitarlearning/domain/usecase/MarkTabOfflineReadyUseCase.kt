package com.guitarlearning.domain.usecase

import com.guitarlearning.domain.repository.TabRepository

class MarkTabOfflineReadyUseCase(
    private val tabRepository: TabRepository
) {
    suspend operator fun invoke(tabId: String, ready: Boolean) {
        tabRepository.markOfflineReady(tabId, ready)
    }
}
