package com.example.thetest1.domain.repository

import com.example.thetest1.domain.model.SearchResult

interface TabSearchRepository {
    suspend fun searchTabs(query: String): Result<List<SearchResult>>
}
