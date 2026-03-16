package com.example.thetest1.data.repository

import android.content.Context
import com.example.thetest1.domain.repository.SoundFontRepository

class SoundFontRepositoryImpl(
    private val context: Context
) : SoundFontRepository {

    override suspend fun readSoundFontBytes(): ByteArray {
        return context.assets.open(SOUND_FONT_PATH).use { it.readBytes() }
    }

    private companion object {
        const val SOUND_FONT_PATH = "sonivox.sf2"
    }
}
