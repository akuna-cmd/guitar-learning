package com.guitarlearning.data.repository

import com.guitarlearning.data.local.dao.GoalDao
import com.guitarlearning.data.local.entity.toDomain
import com.guitarlearning.data.local.entity.toEntity
import com.guitarlearning.domain.model.Goal
import com.guitarlearning.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalRepositoryImpl @Inject constructor(
    private val goalDao: GoalDao
) : GoalRepository {
    override fun getGoals(): Flow<List<Goal>> = goalDao.getGoals().map { goals -> goals.map { it.toDomain() } }

    override suspend fun getGoalsSync(): List<Goal> = goalDao.getGoalsSync().map { it.toDomain() }

    override suspend fun addGoal(goal: Goal) {
        goalDao.addGoal(goal.prepareForPersist().toEntity())
    }

    override suspend fun upsertGoals(goals: List<Goal>) {
        if (goals.isEmpty()) return
        goalDao.upsertGoals(goals.map { it.prepareForPersist(preserveUpdatedAt = true).toEntity() })
    }

    override suspend fun updateGoal(goal: Goal) {
        goalDao.updateGoal(goal.prepareForPersist().toEntity())
    }

    override suspend fun deleteGoal(goal: Goal) {
        goalDao.deleteGoal(goal.toEntity())
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
