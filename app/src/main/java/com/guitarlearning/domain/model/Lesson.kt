package com.guitarlearning.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Lesson(
    val id: String,
    val level: String,
    val order: Int,
    val title: String,
    val description: String,
    val text: String,
    val tabsAscii: String,
    val tabsGpPath: String
)
