package com.example.thetest1.domain.usecase

import com.example.thetest1.domain.repository.TabRepository

class AddUserTabUseCase(private val tabRepository: TabRepository) {
    suspend operator fun invoke(uriString: String) = tabRepository.addUserTab(uriString)
}
