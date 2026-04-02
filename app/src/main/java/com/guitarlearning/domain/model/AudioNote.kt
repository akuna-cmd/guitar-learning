package com.guitarlearning.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import androidx.compose.runtime.Immutable

@Immutable
@Entity(tableName = "audio_notes")
data class AudioNote(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val lessonId: String,
    val filePath: String,
    val createdAt: Date
)
