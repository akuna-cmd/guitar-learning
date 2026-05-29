package com.guitarlearning.domain.usecase

import com.guitarlearning.domain.session.SessionHistoryTransfer
import javax.inject.Inject

class ImportSessionHistoryUseCase @Inject constructor(
    private val sessionHistoryTransfer: SessionHistoryTransfer
) {
    suspend operator fun invoke(content: String) {
        sessionHistoryTransfer.importHistory(content)
    }
}
