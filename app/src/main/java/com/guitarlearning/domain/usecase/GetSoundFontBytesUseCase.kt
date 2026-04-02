package com.guitarlearning.domain.usecase

import com.guitarlearning.domain.repository.SoundFontRepository

class GetSoundFontBytesUseCase(
    private val repository: SoundFontRepository
) {
    suspend operator fun invoke(): Result<ByteArray> {
        return runCatching { repository.readSoundFontBytes() }
    }
}
