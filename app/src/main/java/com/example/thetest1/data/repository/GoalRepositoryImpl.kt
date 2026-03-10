package com.example.thetest1.data.repository

import com.example.thetest1.data.local.GoalDao
import com.example.thetest1.domain.model.Goal
import com.example.thetest1.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow

class GoalRepositoryImpl(private val goalDao: GoalDao) : GoalRepository {
    override fun getGoals(): Flow<List<Goal>> = goalDao.getGoals()

    override suspend fun addGoal(goal: Goal) {
        goalDao.addGoal(goal)
    }

    override suspend fun updateGoal(goal: Goal) {
        goalDao.updateGoal(goal)
    }

    override suspend fun deleteGoal(goal: Goal) {
        goalDao.deleteGoal(goal)
    }
}
