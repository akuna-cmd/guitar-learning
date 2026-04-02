package com.guitarlearning.domain.usecase

import com.guitarlearning.domain.repository.TabRepository

class UpdateTabFolderUseCase(
    private val tabRepository: TabRepository
) {
    suspend operator fun invoke(tabId: String, folder: String) {
        tabRepository.updateTabFolder(tabId, folder)
    }
}
