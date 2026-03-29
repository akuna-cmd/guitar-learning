package com.example.thetest1.domain.usecase

import com.example.thetest1.domain.model.TabItem
import com.example.thetest1.domain.repository.TabRepository
import kotlinx.coroutines.flow.Flow

class ObserveUserTabsUseCase(private val tabRepository: TabRepository) {
    operator fun invoke(): Flow<List<TabItem>> = tabRepository.observeUserTabs()
}
