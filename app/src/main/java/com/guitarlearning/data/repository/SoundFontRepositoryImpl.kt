package com.guitarlearning.data.repository

import android.content.Context
import com.guitarlearning.domain.repository.SoundFontRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundFontRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SoundFontRepository {

    override suspend fun readSoundFontBytes(): ByteArray {
        return context.assets.open(SOUND_FONT_PATH).use { it.readBytes() }
    }

    private companion object {
        const val SOUND_FONT_PATH = "sonivox.sf2"
    }
}
