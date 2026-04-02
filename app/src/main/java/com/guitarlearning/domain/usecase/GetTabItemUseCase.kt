package com.guitarlearning.domain.usecase

import com.guitarlearning.domain.model.TabItem
import com.guitarlearning.domain.repository.TabRepository

class GetTabItemUseCase(private val tabRepository: TabRepository) {
    suspend operator fun invoke(id: String): TabItem? {
        return tabRepository.getTabById(id)
    }
}
