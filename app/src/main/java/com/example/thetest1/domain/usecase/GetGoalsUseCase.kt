package com.example.thetest1.domain.usecase

import com.example.thetest1.domain.model.Goal
import com.example.thetest1.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow

class GetGoalsUseCase(private val goalRepository: GoalRepository) {
    operator fun invoke(): Flow<List<Goal>> = goalRepository.getGoals()
}
