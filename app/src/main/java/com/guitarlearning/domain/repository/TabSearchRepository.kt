package com.guitarlearning.domain.repository

import com.guitarlearning.domain.model.SearchResult

interface TabSearchRepository {
    suspend fun searchTabs(query: String): Result<List<SearchResult>>
}
