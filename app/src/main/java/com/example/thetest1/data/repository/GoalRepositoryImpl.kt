package com.example.thetest1.data.repository

import com.example.thetest1.data.local.GoalDao
import com.example.thetest1.domain.model.Goal
import com.example.thetest1.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalRepositoryImpl @Inject constructor(
    private val goalDao: GoalDao
) : GoalRepository {
    override fun getGoals(): Flow<List<Goal>> = goalDao.getGoals()

    override suspend fun getGoalsSync(): List<Goal> = goalDao.getGoalsSync()

    override suspend fun addGoal(goal: Goal) {
        goalDao.addGoal(goal.prepareForPersist())
    }

    override suspend fun upsertGoals(goals: List<Goal>) {
        if (goals.isEmpty()) return
        goalDao.upsertGoals(goals.map { it.prepareForPersist(preserveUpdatedAt = true) })
    }

    override suspend fun updateGoal(goal: Goal) {
        goalDao.updateGoal(goal.prepareForPersist())
    }

    override suspend fun deleteGoal(goal: Goal) {
        goalDao.deleteGoal(goal)
    }

    override suspend fun clearGoals() {
        goalDao.clearGoals()
    }

    private fun Goal.prepareForPersist(preserveUpdatedAt: Boolean = false): Goal {
        return copy(
            syncId = syncId.ifBlank { UUID.randomUUID().toString() },
            updatedAt = if (preserveUpdatedAt && updatedAt > 0L) updatedAt else System.currentTimeMillis()
        )
    }
}
