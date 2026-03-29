package com.example.thetest1.domain.repository

import com.example.thetest1.domain.model.Goal
import kotlinx.coroutines.flow.Flow

interface GoalRepository {
    fun getGoals(): Flow<List<Goal>>
    suspend fun getGoalsSync(): List<Goal>
    suspend fun addGoal(goal: Goal)
    suspend fun upsertGoals(goals: List<Goal>)
    suspend fun updateGoal(goal: Goal)
    suspend fun deleteGoal(goal: Goal)
    suspend fun clearGoals()
}
