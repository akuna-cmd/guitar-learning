package com.example.thetest1.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val startTime: Date,
    val endTime: Date,
    val duration: Long,
    val practicedTabs: List<PracticedTab> = emptyList()
)

data class PracticedTab(
    val tabId: String,
    val tabName: String,
    val duration: Long
)
