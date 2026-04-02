package com.guitarlearning.domain.repository

interface TabFileRepository {
    suspend fun readTabBytes(path: String): ByteArray
}
