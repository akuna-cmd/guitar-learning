package com.example.thetest1.domain.usecase

import com.example.thetest1.domain.model.Goal
import com.example.thetest1.domain.repository.GoalRepository

class UpdateGoalUseCase(private val goalRepository: GoalRepository) {
    suspend operator fun invoke(goal: Goal) = goalRepository.updateGoal(goal)
}
