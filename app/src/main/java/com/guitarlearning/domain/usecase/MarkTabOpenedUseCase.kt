package com.guitarlearning.domain.usecase

import com.guitarlearning.domain.repository.TabRepository

class MarkTabOpenedUseCase(
    private val tabRepository: TabRepository
) {
    suspend operator fun invoke(tabId: String, openedAt: Long = System.currentTimeMillis()) {
        tabRepository.markTabOpened(tabId, openedAt)
    }
}
