package com.guitarlearning.domain.usecase

import com.guitarlearning.domain.model.Goal
import com.guitarlearning.domain.repository.GoalRepository

class UpdateGoalUseCase(private val goalRepository: GoalRepository) {
    suspend operator fun invoke(goal: Goal) = goalRepository.updateGoal(goal)
}
