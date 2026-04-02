package com.guitarlearning.domain.usecase

import com.guitarlearning.domain.repository.TabRepository
import kotlinx.coroutines.flow.Flow

class GetUserTabsCountUseCase(private val tabRepository: TabRepository) {
    operator fun invoke(): Flow<Int> {
        return tabRepository.getUserTabsCount()
    }
}
