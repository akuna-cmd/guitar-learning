package com.example.thetest1.domain.model

data class AiAssistantRequest(
    val question: String,
    val theory: String,
    val tabs: String,
    val measureRange: IntRange?
)
