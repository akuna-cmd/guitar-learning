package com.guitarlearning.domain.session

interface SessionHistoryTransfer {
    suspend fun exportHistory(): String
    suspend fun importHistory(content: String)
}
