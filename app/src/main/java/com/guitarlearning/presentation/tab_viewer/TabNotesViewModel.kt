package com.guitarlearning.presentation.tab_viewer

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guitarlearning.di.AppDispatchers
import com.guitarlearning.domain.model.AudioNote
import com.guitarlearning.domain.model.TextNote
import com.guitarlearning.domain.repository.AudioNoteRepository
import com.guitarlearning.domain.repository.TextNoteRepository
import com.guitarlearning.presentation.audio_notes.AudioNoteMediaController
import com.guitarlearning.presentation.audio_notes.PlayerState
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val audioNoteRepository: AudioNoteRepository,
    private val textNoteRepository: TextNoteRepository,
    private val mediaController: AudioNoteMediaController,
    private val dispatchers: AppDispatchers
) : ViewModel() {

    private val _uiState = MutableStateFlow(TabNotesUiState())
    val uiState: StateFlow<TabNotesUiState> = _uiState.asStateFlow()

    private var audioFile: File? = null
    private var audioNotesJob: Job? = null
    private var textNotesJob: Job? = null
    private var boundLessonId: String? = null

    init {
        viewModelScope.launch {
            mediaController.playerState.collect { playerState ->
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
            val newFile = mediaController.importAudio(uri) ?: return@launch
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
            mediaController.stopRecording()?.let { recordedFile ->
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
            audioFile = null
            _uiState.update { it.copy(isRecording = false) }
        } else {
            audioFile = mediaController.startRecording()
            _uiState.update { it.copy(isRecording = true) }
        }
    }

    fun deleteAudioNote(audioNoteId: Int) {
        viewModelScope.launch(dispatchers.io) {
            audioNoteRepository.deleteAudioNote(audioNoteId)
        }
    }

    fun toggleAudioFavorite(audioNote: AudioNote) {
        viewModelScope.launch(dispatchers.io) {
            audioNoteRepository.updateAudioNote(audioNote.copy(isFavorite = !audioNote.isFavorite))
        }
    }

    fun renameAudioNote(audioNote: AudioNote, newName: String) {
        viewModelScope.launch(dispatchers.io) {
            val trimmedName = newName.trim()
            if (trimmedName.isBlank()) return@launch
            val sourceFile = File(audioNote.filePath)
            if (!sourceFile.exists()) return@launch
            val extension = sourceFile.extension.takeIf { it.isNotBlank() }?.let { ".$it" }.orEmpty()
            val normalizedName = if (trimmedName.endsWith(extension, ignoreCase = true) || extension.isEmpty()) {
                trimmedName
            } else {
                trimmedName + extension
            }
            val targetFile = File(sourceFile.parentFile ?: return@launch, normalizedName)
            if (targetFile.absolutePath != sourceFile.absolutePath && sourceFile.renameTo(targetFile)) {
                audioNoteRepository.updateAudioNote(audioNote.copy(filePath = targetFile.absolutePath))
            }
        }
    }

    fun onPlayAudio(audioNote: AudioNote) {
        mediaController.play(audioNote.id.toString(), audioNote.filePath)
    }

    fun onSeekAudio(trackId: String, progress: Float) {
        mediaController.seek(trackId, progress)
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

    fun toggleTextFavorite(textNote: TextNote) {
        viewModelScope.launch(dispatchers.io) {
            textNoteRepository.updateTextNote(textNote.copy(isFavorite = !textNote.isFavorite))
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaController.release()
    }
}
