package com.guitarlearning.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.guitarlearning.domain.model.GoalType

@Entity(
    tableName = "goals",
    indices = [Index(value = ["syncId"], unique = true)]
)
data class GoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val syncId: String,
    val type: GoalType,
    val description: String,
    val target: Int,
    val progress: Int = 0,
    val deadline: Long,
    val updatedAt: Long = 0L,
    val isCompleted: Boolean = false,
    val isOverdue: Boolean = false
)
