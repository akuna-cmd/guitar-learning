package com.example.thetest1.domain.usecase

import com.example.thetest1.domain.model.TabItem
import com.example.thetest1.domain.repository.TabRepository
import kotlinx.coroutines.flow.first

class GetTabsUseCase(private val tabRepository: TabRepository) {
    suspend operator fun invoke(): List<TabItem> {
        return tabRepository.getTabs().first()
    }
}
