package com.guitarlearning.presentation.audio_notes

import android.content.Context
import android.net.Uri
import com.guitarlearning.di.AppDispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class AudioNoteMediaController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: AppDispatchers
) {
    private val audioRecorder = AudioRecorder(context)
    private val audioPlayer = AudioPlayer(context, dispatchers.main)
    private var recordingFile: File? = null

    val playerState: StateFlow<PlayerState> = audioPlayer.playerState

    suspend fun importAudio(uri: Uri): File? = withContext(dispatchers.io) {
        val targetFile = File(context.filesDir, "audio_${System.currentTimeMillis()}.mp4")
        val copied = runCatching {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                targetFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            targetFile
        }.getOrNull()
        copied?.takeIf { it.exists() && it.length() > 0L }
    }

    fun startRecording(): File {
        val targetFile = File(context.filesDir, "audio_${System.currentTimeMillis()}.mp4")
        audioRecorder.start(targetFile)
        recordingFile = targetFile
        return targetFile
    }

    fun stopRecording(): File? {
        return runCatching {
            audioRecorder.stop()
            recordingFile
        }.getOrNull()
            ?.takeIf { it.exists() && it.length() > 0L }
            .also { recordingFile = null }
    }

    fun play(trackId: String, filePath: String) {
        audioPlayer.onPlay(trackId, filePath)
    }

    fun seek(trackId: String, progress: Float) {
        audioPlayer.seek(trackId, progress)
    }

    fun release() {
        audioPlayer.release()
    }
}
