package com.guitarlearning.domain.usecase

import com.guitarlearning.domain.repository.TabFileRepository

class GetTabFileBytesUseCase(
    private val repository: TabFileRepository
) {
    suspend operator fun invoke(path: String): Result<ByteArray> {
        return runCatching { repository.readTabBytes(path) }
    }
}
