package com.guitarlearning.domain.usecase

import com.guitarlearning.domain.model.TabItem
import com.guitarlearning.domain.repository.TabRepository
import kotlinx.coroutines.flow.Flow

class ObserveUserTabsUseCase(private val tabRepository: TabRepository) {
    operator fun invoke(): Flow<List<TabItem>> = tabRepository.observeUserTabs()
}
