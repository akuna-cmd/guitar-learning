package com.guitarlearning.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class AiAssistantRequest(
    val question: String,
    val theory: String,
    val tabs: String,
    val measureRange: IntRange?
)
