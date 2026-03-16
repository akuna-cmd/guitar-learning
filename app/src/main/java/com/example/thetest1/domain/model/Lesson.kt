package com.example.thetest1.domain.model

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
