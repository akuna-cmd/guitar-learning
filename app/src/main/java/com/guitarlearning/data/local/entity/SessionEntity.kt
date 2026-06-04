package com.guitarlearning.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val startTime: Date,
    val endTime: Date
)

@Entity(
    tableName = "practiced_tabs",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TabEntity::class,
            parentColumns = ["id"],
            childColumns = ["tabId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId"), Index("tabId")]
)
data class PracticedTabEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val sessionId: Int,
    val tabId: String,
    val duration: Long
)

data class SessionRow(
    val sessionId: Int,
    val startTime: Date,
    val endTime: Date,
    val practicedTabEntryId: Int?,
    val tabId: String?,
    val tabName: String?,
    val practicedDuration: Long?
)

