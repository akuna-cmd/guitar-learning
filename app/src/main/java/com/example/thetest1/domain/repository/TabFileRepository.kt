package com.example.thetest1.domain.repository

interface TabFileRepository {
    suspend fun readTabBytes(path: String): ByteArray
}
