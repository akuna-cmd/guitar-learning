package com.example.thetest1.domain.usecase

import com.example.thetest1.domain.model.TabItem
import com.example.thetest1.domain.repository.TabRepository

class UpdateTabUseCase(private val tabRepository: TabRepository) {
    suspend operator fun invoke(tab: TabItem) {
        tabRepository.updateTab(tab)
    }
}