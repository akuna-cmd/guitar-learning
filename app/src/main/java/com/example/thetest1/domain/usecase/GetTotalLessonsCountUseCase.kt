package com.example.thetest1.domain.usecase

import com.example.thetest1.domain.repository.TabRepository
import kotlinx.coroutines.flow.Flow

class GetTotalLessonsCountUseCase(private val tabRepository: TabRepository) {
    operator fun invoke(): Flow<Int> = tabRepository.getTotalLessonsCount()
}
