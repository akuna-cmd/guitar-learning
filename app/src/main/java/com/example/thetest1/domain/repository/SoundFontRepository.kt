package com.example.thetest1.domain.repository

interface SoundFontRepository {
    suspend fun readSoundFontBytes(): ByteArray
}
