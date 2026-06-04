package com.guitarlearning.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "text_notes",
    indices = [Index("lessonId")]
)
data class TextNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val lessonId: String,
    val content: String,
    val createdAt: Date,
    val isFavorite: Boolean = false
)
