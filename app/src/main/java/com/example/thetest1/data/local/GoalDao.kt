package com.example.thetest1.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.thetest1.domain.model.Goal
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals")
    fun getGoals(): Flow<List<Goal>>

    @Query("SELECT * FROM goals")
    suspend fun getGoalsSync(): List<Goal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addGoal(goal: Goal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoals(goals: List<Goal>)

    @Update
    suspend fun updateGoal(goal: Goal)

    @Delete
    suspend fun deleteGoal(goal: Goal)

    @Query("DELETE FROM goals")
    suspend fun clearGoals()
}
