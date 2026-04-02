package com.guitarlearning.domain.usecase

import com.guitarlearning.domain.repository.TabRepository
import kotlinx.coroutines.flow.Flow

class GetCompletedLessonsCountUseCase(private val tabRepository: TabRepository) {
    operator fun invoke(): Flow<Int> = tabRepository.getCompletedLessonsCount()
}
