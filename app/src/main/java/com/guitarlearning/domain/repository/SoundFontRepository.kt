package com.guitarlearning.domain.repository

interface SoundFontRepository {
    suspend fun readSoundFontBytes(): ByteArray
}
