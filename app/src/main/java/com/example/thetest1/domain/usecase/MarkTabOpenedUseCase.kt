package com.example.thetest1.domain.usecase

import com.example.thetest1.domain.repository.TabRepository

class MarkTabOpenedUseCase(
    private val tabRepository: TabRepository
) {
    suspend operator fun invoke(tabId: String, openedAt: Long = System.currentTimeMillis()) {
        tabRepository.markTabOpened(tabId, openedAt)
    }
}
