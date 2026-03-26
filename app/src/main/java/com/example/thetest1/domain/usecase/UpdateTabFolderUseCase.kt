package com.example.thetest1.domain.usecase

import com.example.thetest1.domain.repository.TabRepository

class UpdateTabFolderUseCase(
    private val tabRepository: TabRepository
) {
    suspend operator fun invoke(tabId: String, folder: String) {
        tabRepository.updateTabFolder(tabId, folder)
    }
}
