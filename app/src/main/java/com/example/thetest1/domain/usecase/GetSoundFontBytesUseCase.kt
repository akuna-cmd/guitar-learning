package com.example.thetest1.domain.usecase

import com.example.thetest1.domain.repository.SoundFontRepository

class GetSoundFontBytesUseCase(
    private val repository: SoundFontRepository
) {
    suspend operator fun invoke(): Result<ByteArray> {
        return runCatching { repository.readSoundFontBytes() }
    }
}
