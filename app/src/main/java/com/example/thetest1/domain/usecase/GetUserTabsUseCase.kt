package com.example.thetest1.domain.usecase

import com.example.thetest1.domain.model.TabItem
import com.example.thetest1.domain.repository.TabRepository

class GetUserTabsUseCase(private val tabRepository: TabRepository) {
    suspend operator fun invoke(): List<TabItem> = tabRepository.getUserTabs()
}
