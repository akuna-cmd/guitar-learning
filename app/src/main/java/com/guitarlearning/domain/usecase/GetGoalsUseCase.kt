package com.guitarlearning.domain.usecase

import com.guitarlearning.domain.model.Goal
import com.guitarlearning.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow

class GetGoalsUseCase(private val goalRepository: GoalRepository) {
    operator fun invoke(): Flow<List<Goal>> = goalRepository.getGoals()
}
