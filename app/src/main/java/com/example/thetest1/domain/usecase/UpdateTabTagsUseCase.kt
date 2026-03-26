package com.example.thetest1.domain.usecase

import com.example.thetest1.domain.repository.TabRepository

class UpdateTabTagsUseCase(
    private val tabRepository: TabRepository
) {
    suspend operator fun invoke(tabId: String, tags: List<String>) {
        tabRepository.updateTabTags(tabId, tags)
    }
}
