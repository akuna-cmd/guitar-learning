package com.guitarlearning.core.session

import android.net.Uri

interface SessionHistoryTransfer {
    suspend fun exportHistory(target: Uri)
    suspend fun importHistory(source: Uri)
}
