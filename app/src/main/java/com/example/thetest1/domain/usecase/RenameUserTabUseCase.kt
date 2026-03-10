package com.example.thetest1.domain.usecase

import com.example.thetest1.domain.model.TabItem
import com.example.thetest1.domain.repository.TabRepository

class RenameUserTabUseCase(private val tabRepository: TabRepository) {
    suspend operator fun invoke(tab: TabItem, newName: String) {
        tabRepository.renameUserTab(tab, newName)
    }
}