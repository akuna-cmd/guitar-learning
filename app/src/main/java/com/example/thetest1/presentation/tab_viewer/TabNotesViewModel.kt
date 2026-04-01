package com.example.thetest1.presentation.tab_viewer

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thetest1.di.AppDispatchers
import com.example.thetest1.domain.model.AudioNote
import com.example.thetest1.domain.model.TextNote
import com.example.thetest1.domain.repository.AudioNoteRepository
import com.example.thetest1.domain.repository.TextNoteRepository
import com.example.thetest1.presentation.audio_notes.AudioPlayer
import com.example.thetest1.presentation.audio_notes.AudioRecorder
import com.example.thetest1.presentation.audio_notes.PlayerState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.Date
import javax.inject.Inject

data class TabNotesUiState(
    val audioNotes: List<AudioNote> = emptyList(),
    val textNotes: List<TextNote> = emptyList(),
    val isRecording: Boolean = false,
    val playerState: PlayerState = PlayerState()
)

@HiltViewModel
class TabNotesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioNoteRepository: AudioNoteRepository,
    private val textNoteRepository: TextNoteRepository,
    private val dispatchers: AppDispatchers
) : ViewModel() {

    private val _uiState = MutableStateFlow(TabNotesUiState())
    val uiState: StateFlow<TabNotesUiState> = _uiState.asStateFlow()

    private val audioRecorder by lazy { AudioRecorder(context) }
    private val audioPlayer by lazy { AudioPlayer(context, dispatchers.main) }
    private var audioFile: File? = null
    private var audioNotesJob: Job? = null
    private var textNotesJob: Job? = null
    private var boundLessonId: String? = null

    init {
        viewModelScope.launch {
            audioPlayer.playerState.collect { playerState ->
                _uiState.update { it.copy(playerState = playerState) }
            }
        }
    }

    fun bindLesson(lessonId: String) {
        if (boundLessonId == lessonId) return
        boundLessonId = lessonId
        audioNotesJob?.cancel()
        textNotesJob?.cancel()
        _uiState.update {
            it.copy(
                audioNotes = emptyList(),
                textNotes = emptyList(),
                isRecording = false
            )
        }
        audioNotesJob = audioNoteRepository.getAudioNotes(lessonId)
            .onEach { audioNotes ->
                _uiState.update { it.copy(audioNotes = audioNotes) }
            }
            .launchIn(viewModelScope)
        textNotesJob = textNoteRepository.getTextNotes(lessonId)
            .onEach { textNotes ->
                _uiState.update { it.copy(textNotes = textNotes) }
            }
            .launchIn(viewModelScope)
    }

    fun addAudioNoteFromFile(lessonId: String, uri: Uri) {
        viewModelScope.launch(dispatchers.io) {
            val fileName = "audio_${System.currentTimeMillis()}.mp4"
            val newFile = File(context.filesDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                newFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            audioNoteRepository.addAudioNote(
                AudioNote(
                    lessonId = lessonId,
                    filePath = newFile.absolutePath,
                    createdAt = Date()
                )
            )
        }
    }

    fun onRecordAudio(lessonId: String) {
        if (_uiState.value.isRecording) {
            audioRecorder.stop()
            audioFile?.let { recordedFile ->
                viewModelScope.launch(dispatchers.io) {
                    audioNoteRepository.addAudioNote(
                        AudioNote(
                            lessonId = lessonId,
                            filePath = recordedFile.absolutePath,
                            createdAt = Date()
                        )
                    )
                }
            }
            _uiState.update { it.copy(isRecording = false) }
        } else {
            val fileName = "audio_${System.currentTimeMillis()}.mp4"
            val newFile = File(context.filesDir, fileName)
            audioRecorder.start(newFile)
            audioFile = newFile
            _uiState.update { it.copy(isRecording = true) }
        }
    }

    fun deleteAudioNote(audioNoteId: Int) {
        viewModelScope.launch(dispatchers.io) {
            audioNoteRepository.deleteAudioNote(audioNoteId)
        }
    }

    fun onPlayAudio(audioNote: AudioNote) {
        audioPlayer.onPlay(audioNote.id.toString(), audioNote.filePath)
    }

    fun onSeekAudio(trackId: String, progress: Float) {
        audioPlayer.seek(trackId, progress)
    }

    fun addTextNote(lessonId: String, content: String) {
        viewModelScope.launch(dispatchers.io) {
            textNoteRepository.addTextNote(
                TextNote(
                    lessonId = lessonId,
                    content = content,
                    createdAt = Date()
                )
            )
        }
    }

    fun updateTextNote(textNote: TextNote) {
        viewModelScope.launch(dispatchers.io) {
            textNoteRepository.updateTextNote(textNote)
        }
    }

    fun deleteTextNote(textNote: TextNote) {
        viewModelScope.launch(dispatchers.io) {
            textNoteRepository.deleteTextNote(textNote)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
}
