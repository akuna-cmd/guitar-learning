package com.guitarlearning.domain.model

import androidx.compose.runtime.Immutable
import java.util.Date

@Immutable
data class AudioNote(
    val id: Int = 0,
    val lessonId: String,
    val filePath: String,
    val createdAt: Date,
    val isFavorite: Boolean = false
)
