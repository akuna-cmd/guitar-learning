package com.example.thetest1.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "text_notes")
data class TextNote(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val lessonId: String,
    val content: String,
    val createdAt: Date
)
