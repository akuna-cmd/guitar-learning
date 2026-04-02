package com.guitarlearning.domain.usecase

import com.guitarlearning.domain.model.TabItem
import com.guitarlearning.domain.repository.TabRepository

class DeleteUserTabUseCase(private val tabRepository: TabRepository) {
    suspend operator fun invoke(tab: TabItem) {
        tabRepository.deleteUserTab(tab)
    }
}
