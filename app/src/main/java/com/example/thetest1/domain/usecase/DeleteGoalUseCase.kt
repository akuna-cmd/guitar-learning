package com.example.thetest1.domain.usecase

import com.example.thetest1.domain.model.Goal
import com.example.thetest1.domain.repository.GoalRepository

class DeleteGoalUseCase(private val goalRepository: GoalRepository) {
    suspend operator fun invoke(goal: Goal) = goalRepository.deleteGoal(goal)
}
