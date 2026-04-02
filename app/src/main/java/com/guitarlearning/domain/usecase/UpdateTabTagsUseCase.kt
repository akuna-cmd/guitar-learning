package com.guitarlearning.domain.usecase

import com.guitarlearning.domain.repository.TabRepository

class UpdateTabTagsUseCase(
    private val tabRepository: TabRepository
) {
    suspend operator fun invoke(tabId: String, tags: List<String>) {
        tabRepository.updateTabTags(tabId, tags)
    }
}
