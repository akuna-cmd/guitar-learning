package com.guitarlearning.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "audio_notes",
    indices = [Index("lessonId")]
)
data class AudioNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val lessonId: String,
    val filePath: String,
    val createdAt: Date,
    val isFavorite: Boolean = false
)
