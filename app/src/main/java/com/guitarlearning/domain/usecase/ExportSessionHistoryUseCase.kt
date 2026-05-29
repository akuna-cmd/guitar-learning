package com.guitarlearning.domain.usecase

import com.guitarlearning.domain.session.SessionHistoryTransfer
import javax.inject.Inject

class ExportSessionHistoryUseCase @Inject constructor(
    private val sessionHistoryTransfer: SessionHistoryTransfer
) {
    suspend operator fun invoke(): String = sessionHistoryTransfer.exportHistory()
}
