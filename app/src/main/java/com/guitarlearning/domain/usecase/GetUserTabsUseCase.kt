package com.guitarlearning.domain.usecase

import com.guitarlearning.domain.model.TabItem
import com.guitarlearning.domain.repository.TabRepository

class GetUserTabsUseCase(private val tabRepository: TabRepository) {
    suspend operator fun invoke(): List<TabItem> = tabRepository.getUserTabs()
}
