package com.guitarlearning.domain.usecase

import com.guitarlearning.domain.model.TabItem
import com.guitarlearning.domain.repository.TabRepository
import kotlinx.coroutines.flow.first

class GetTabsUseCase(private val tabRepository: TabRepository) {
    suspend operator fun invoke(): List<TabItem> {
        return tabRepository.getTabs().first()
    }
}
