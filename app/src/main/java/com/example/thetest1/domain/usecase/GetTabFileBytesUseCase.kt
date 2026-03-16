package com.example.thetest1.domain.usecase

import com.example.thetest1.domain.repository.TabFileRepository

class GetTabFileBytesUseCase(
    private val repository: TabFileRepository
) {
    suspend operator fun invoke(path: String): Result<ByteArray> {
        return runCatching { repository.readTabBytes(path) }
    }
}
