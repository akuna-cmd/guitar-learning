package com.guitarlearning.domain.usecase

import com.guitarlearning.domain.repository.TabRepository

class AddUserTabUseCase(private val tabRepository: TabRepository) {
    suspend operator fun invoke(uriString: String) = tabRepository.addUserTab(uriString)
}
