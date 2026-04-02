package com.guitarlearning.domain.usecase

import com.guitarlearning.domain.model.Goal
import com.guitarlearning.domain.repository.GoalRepository

class AddGoalUseCase(private val goalRepository: GoalRepository) {
    suspend operator fun invoke(goal: Goal) = goalRepository.addGoal(goal)
}
