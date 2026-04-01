package com.example.thetest1.domain.usecase

import com.example.thetest1.domain.model.TabItem
import com.example.thetest1.domain.repository.TabRepository

class GetTabItemUseCase(private val tabRepository: TabRepository) {
    suspend operator fun invoke(id: String): TabItem? {
        return tabRepository.getTabById(id)
    }
}
