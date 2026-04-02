package com.guitarlearning.domain.usecase

import com.guitarlearning.domain.model.TabItem
import com.guitarlearning.domain.repository.TabRepository

class RenameUserTabUseCase(private val tabRepository: TabRepository) {
    suspend operator fun invoke(tab: TabItem, newName: String) {
        tabRepository.renameUserTab(tab, newName)
    }
}
