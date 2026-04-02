package com.guitarlearning.domain.model

data class SearchResult(
    val id: String,
    val title: String,
    val artist: String,
    val hasChords: Boolean
)
