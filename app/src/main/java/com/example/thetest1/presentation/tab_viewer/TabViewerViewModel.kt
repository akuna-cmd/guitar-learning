package com.example.thetest1.presentation.tab_viewer

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thetest1.data.model.Lesson
import com.example.thetest1.domain.model.AudioNote
import com.example.thetest1.domain.model.TextNote
import com.example.thetest1.domain.usecase.AddAudioNoteUseCase
import com.example.thetest1.domain.usecase.AddTextNoteUseCase
import com.example.thetest1.domain.usecase.DeleteAudioNoteUseCase
import com.example.thetest1.domain.usecase.DeleteTextNoteUseCase
import com.example.thetest1.domain.usecase.GetAudioNotesUseCase
import com.example.thetest1.domain.usecase.GetLessonUseCase
import com.example.thetest1.domain.usecase.GetTabItemUseCase
import com.example.thetest1.domain.usecase.GetTextNotesUseCase
import com.example.thetest1.domain.usecase.UpdateTextNoteUseCase
import com.example.thetest1.presentation.audio_notes.AudioPlayer
import com.example.thetest1.presentation.audio_notes.AudioRecorder
import com.example.thetest1.presentation.audio_notes.PlayerState
import com.google.gson.Gson
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.Date

enum class LessonTab {
    THEORY,
    TABS,
    AI_ASSISTANT,
    NOTES
}

data class FingerInfo(
    val finger: String,
    val fingerName: String,
    val string: String,
    val fret: String? = null,
    val direction: String? = null,
    val color: String
)

data class TabAnalysis(
    val barIndex: Int,
    val leftHand: List<FingerInfo>,
    val rightHand: List<FingerInfo>,
    val instructions: List<String>
)

data class TabViewerUiState(
    val lesson: Lesson? = null,
    val isUserTab: Boolean = false,
    val tabs: List<LessonTab> = LessonTab.values().toList(),
    val selectedTab: LessonTab = LessonTab.TABS,
    val selectedTabIndex: Int = LessonTab.values().toList().indexOf(LessonTab.TABS),
    val audioNotes: List<AudioNote> = emptyList(),
    val textNotes: List<TextNote> = emptyList(),
    val isRecording: Boolean = false,
    val playerState: PlayerState = PlayerState(),
    val asciiTab: String? = null,
    val tabAnalysis: TabAnalysis? = null
)

class TabViewerViewModel(
    private val context: Context,
    private val getLessonUseCase: GetLessonUseCase,
    private val getTabItemUseCase: GetTabItemUseCase,
    private val getAudioNotesUseCase: GetAudioNotesUseCase,
    private val addAudioNoteUseCase: AddAudioNoteUseCase,
    private val deleteAudioNoteUseCase: DeleteAudioNoteUseCase,
    private val getTextNotesUseCase: GetTextNotesUseCase,
    private val addTextNoteUseCase: AddTextNoteUseCase,
    private val updateTextNoteUseCase: UpdateTextNoteUseCase,
    private val deleteTextNoteUseCase: DeleteTextNoteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TabViewerUiState())
    val uiState: StateFlow<TabViewerUiState> = _uiState.asStateFlow()

    private val audioRecorder by lazy { AudioRecorder(context) }
    private val audioPlayer by lazy { AudioPlayer(context) }
    private var audioFile: File? = null
    private var audioNotesJob: Job? = null
    private var textNotesJob: Job? = null
    private val gson = Gson()

    init {
        viewModelScope.launch {
            audioPlayer.playerState.collect { playerState ->
                _uiState.update { it.copy(playerState = playerState) }
            }
        }
    }

    fun loadLesson(id: String) {
        viewModelScope.launch {
            val lesson = getLessonUseCase(id)
            val tabItem = getTabItemUseCase(id)
            _uiState.update { currentState ->
                val isUserTab = tabItem?.isUserTab == true
                val tabs = if (isUserTab) {
                    LessonTab.values().filter { it != LessonTab.THEORY }
                } else {
                    LessonTab.values().toList()
                }

                val selectedTab = if (currentState.selectedTab in tabs) {
                    currentState.selectedTab
                } else {
                    tabs.firstOrNull() ?: LessonTab.TABS
                }
                val selectedTabIndex = tabs.indexOf(selectedTab)

                currentState.copy(
                    lesson = lesson,
                    isUserTab = isUserTab,
                    tabs = tabs,
                    selectedTab = selectedTab,
                    selectedTabIndex = if (selectedTabIndex != -1) selectedTabIndex else 0
                )
            }
            lesson?.let {
                audioNotesJob?.cancel()
                audioNotesJob = viewModelScope.launch {
                    getAudioNotesUseCase(it.id).collect { audioNotes ->
                        _uiState.update { it.copy(audioNotes = audioNotes) }
                    }
                }
                textNotesJob?.cancel()
                textNotesJob = viewModelScope.launch {
                    getTextNotesUseCase(it.id).collect { textNotes ->
                        _uiState.update { it.copy(textNotes = textNotes) }
                    }
                }
            }
        }
    }

    fun setAsciiTab(ascii: String) {
        _uiState.update { it.copy(asciiTab = ascii) }
    }

    fun setTabAnalysis(analysisJson: String) {
        Log.d("TabViewerViewModel", "Received analysis JSON: $analysisJson")
        try {
            val analysis = gson.fromJson(analysisJson, TabAnalysis::class.java)
            Log.d("TabViewerViewModel", "Parsed analysis: $analysis")
            _uiState.update { it.copy(tabAnalysis = analysis) }
        } catch (e: Exception) {
            Log.e("TabViewerViewModel", "Error parsing analysis", e)
        }
    }

    fun onPlayAudio(audioNote: AudioNote) {
        audioPlayer.onPlay(audioNote.id.toString(), audioNote.filePath)
    }

    fun onSeekAudio(trackId: String, progress: Float) {
        audioPlayer.seek(trackId, progress)
    }

    fun addAudioNoteFromFile(lessonId: String, uri: Uri) {
        viewModelScope.launch {
            val fileName = "audio_${System.currentTimeMillis()}.mp4"
            val newFile = File(context.filesDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                newFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            val audioNote =
                AudioNote(lessonId = lessonId, filePath = newFile.absolutePath, createdAt = Date())
            addAudioNoteUseCase(audioNote)
        }
    }

    fun onRecordAudio(lessonId: String) {
        if (_uiState.value.isRecording) {
            audioRecorder.stop()
            audioFile?.let {
                val audioNote =
                    AudioNote(lessonId = lessonId, filePath = it.absolutePath, createdAt = Date())
                viewModelScope.launch { addAudioNoteUseCase(audioNote) }
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
        viewModelScope.launch {
            deleteAudioNoteUseCase(audioNoteId)
        }
    }

    fun addTextNote(lessonId: String, content: String) {
        viewModelScope.launch {
            val textNote = TextNote(lessonId = lessonId, content = content, createdAt = Date())
            addTextNoteUseCase(textNote)
        }
    }

    fun updateTextNote(textNote: TextNote) {
        viewModelScope.launch {
            updateTextNoteUseCase(textNote)
        }
    }

    fun deleteTextNote(textNote: TextNote) {
        viewModelScope.launch {
            deleteTextNoteUseCase(textNote)
        }
    }

    fun selectTab(tab: LessonTab) {
        _uiState.update { currentState ->
            val selectedIndex = currentState.tabs.indexOf(tab)
            if (selectedIndex != -1) {
                currentState.copy(
                    selectedTab = tab,
                    selectedTabIndex = selectedIndex
                )
            } else {
                currentState
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
}
