package com.example.thetest1.presentation.tab_viewer

import android.content.Context
import android.net.Uri
import android.util.Log
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thetest1.domain.model.Lesson
import com.example.thetest1.domain.model.AudioNote
import com.example.thetest1.domain.model.TextNote
import com.example.thetest1.domain.usecase.AddAudioNoteUseCase
import com.example.thetest1.domain.usecase.AddTextNoteUseCase
import com.example.thetest1.domain.usecase.DeleteAudioNoteUseCase
import com.example.thetest1.domain.usecase.DeleteTextNoteUseCase
import com.example.thetest1.domain.usecase.GetAudioNotesUseCase
import com.example.thetest1.domain.usecase.GetLessonUseCase
import com.example.thetest1.domain.usecase.GetSoundFontBytesUseCase
import com.example.thetest1.domain.usecase.GetTabFileBytesUseCase
import com.example.thetest1.domain.usecase.GetTabItemUseCase
import com.example.thetest1.domain.usecase.GetTextNotesUseCase
import com.example.thetest1.domain.usecase.UpdateTextNoteUseCase
import com.example.thetest1.domain.usecase.GetTabPlaybackProgressUseCase
import com.example.thetest1.domain.usecase.UpdateTabPlaybackProgressUseCase
import com.example.thetest1.domain.model.TabPlaybackProgress
import com.example.thetest1.presentation.audio_notes.AudioPlayer
import com.example.thetest1.presentation.audio_notes.AudioRecorder
import com.example.thetest1.presentation.audio_notes.PlayerState
import com.google.gson.Gson
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.Date
import kotlin.math.abs

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
    val stringIndex: Int? = null,
    val fret: String? = null,
    val direction: String? = null,
    val color: String,
    // Advanced articulations
    val isHammer: Boolean = false,
    val isPullOff: Boolean = false,
    val isSlide: Boolean = false,
    val isVibrato: Boolean = false,
    val isGhost: Boolean = false
)

data class TabAnalysis(
    val barIndex: Int,
    val leftHand: List<FingerInfo>,
    val rightHand: List<FingerInfo>,
    val instructions: List<String>,
    // Advanced context
    val barreFret: Int? = null,
    val nextLeftHand: List<FingerInfo> = emptyList(),
    val contextHint: String? = null
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
    val compactTabs: String? = null,
    val tabAnalysis: TabAnalysis? = null,
    val tabBytesBase64: String? = null,
    val tabBytesPath: String? = null,
    val soundFontBase64: String? = null,
    val lastTickPosition: Long? = null,
    val lastBarIndex: Int? = null,
    val totalBars: Int? = null,
    val restoreTickPosition: Long? = null,
    val restoreBarIndex: Int? = null,
    val wasPlaying: Boolean = false,
    val restorePending: Boolean = false
)

class TabViewerViewModel(
    private val context: Context,
    private val getLessonUseCase: GetLessonUseCase,
    private val getTabItemUseCase: GetTabItemUseCase,
    private val getTabFileBytesUseCase: GetTabFileBytesUseCase,
    private val getSoundFontBytesUseCase: GetSoundFontBytesUseCase,
    private val getAudioNotesUseCase: GetAudioNotesUseCase,
    private val addAudioNoteUseCase: AddAudioNoteUseCase,
    private val deleteAudioNoteUseCase: DeleteAudioNoteUseCase,
    private val getTextNotesUseCase: GetTextNotesUseCase,
    private val addTextNoteUseCase: AddTextNoteUseCase,
    private val updateTextNoteUseCase: UpdateTextNoteUseCase,
    private val deleteTextNoteUseCase: DeleteTextNoteUseCase,
    private val getTabPlaybackProgressUseCase: GetTabPlaybackProgressUseCase,
    private val updateTabPlaybackProgressUseCase: UpdateTabPlaybackProgressUseCase
) : ViewModel() {
    private companion object {
        const val RESTORE_TAG = "TabRestoreFlow"
    }

    private val _uiState = MutableStateFlow(TabViewerUiState())
    val uiState: StateFlow<TabViewerUiState> = _uiState.asStateFlow()

    private val audioRecorder by lazy { AudioRecorder(context) }
    private val audioPlayer by lazy { AudioPlayer(context) }
    private var audioFile: File? = null
    private var audioNotesJob: Job? = null
    private var textNotesJob: Job? = null
    private val gson = Gson()
    private var lastSavedBarIndex: Int = -1
    private var lastSavedAt: Long = 0L

    init {
        viewModelScope.launch {
            audioPlayer.playerState.collect { playerState ->
                _uiState.update { it.copy(playerState = playerState) }
            }
        }
        loadSoundFont()
    }

    fun loadLesson(id: String) {
        viewModelScope.launch {
            val lesson = getLessonUseCase(id)
            val tabItem = getTabItemUseCase(id)
            val savedProgress = getTabPlaybackProgressUseCase(id)
            val shouldRestore = savedProgress?.lastTick?.let { it > 0L } == true
            Log.d(
                RESTORE_TAG,
                "loadLesson(id=$id) savedProgress={tick=${savedProgress?.lastTick}, bar=${savedProgress?.lastBarIndex}, total=${savedProgress?.totalBars}} shouldRestore=$shouldRestore"
            )
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
                    selectedTabIndex = if (selectedTabIndex != -1) selectedTabIndex else 0,
                    lastTickPosition = savedProgress?.lastTick,
                    lastBarIndex = savedProgress?.lastBarIndex,
                    totalBars = savedProgress?.totalBars,
                    restoreTickPosition = savedProgress?.lastTick,
                    restoreBarIndex = savedProgress?.lastBarIndex,
                    wasPlaying = false,
                    restorePending = shouldRestore
                )
            }
            val tabPath = lesson?.tabsGpPath ?: tabItem?.filePath
            if (tabPath != null) {
                loadTabBytes(tabPath)
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

    fun loadTabBytes(path: String) {
        if (_uiState.value.tabBytesPath == path) return
        viewModelScope.launch(Dispatchers.IO) {
            val base64 = getTabFileBytesUseCase(path).getOrNull()?.let { bytes ->
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            }
            _uiState.update { it.copy(tabBytesBase64 = base64, tabBytesPath = path) }
        }
    }

    private fun loadSoundFont() {
        if (_uiState.value.soundFontBase64 != null) return
        viewModelScope.launch(Dispatchers.IO) {
            val base64 = getSoundFontBytesUseCase().getOrNull()?.let { bytes ->
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            }
            _uiState.update { it.copy(soundFontBase64 = base64) }
        }
    }

    fun setAsciiTab(ascii: String) {
        _uiState.update { it.copy(asciiTab = ascii) }
    }

    fun setCompactTabs(tabs: String) {
        _uiState.update { it.copy(compactTabs = tabs) }
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

    fun updatePlaybackState(tick: Long, isPlaying: Boolean) {
        _uiState.update { state ->
            state.copy(
                lastTickPosition = tick,
                wasPlaying = isPlaying
            )
        }
    }

    fun markRestoreCompleted() {
        Log.d(RESTORE_TAG, "markRestoreCompleted()")
        _uiState.update { state ->
            if (!state.restorePending) state else state.copy(restorePending = false)
        }
    }

    fun onRestoreApplied(tick: Long, currentBarIndex: Int, requestedBarIndex: Int) {
        _uiState.update { state ->
            if (!state.restorePending) return@update state
            val expectedBar = state.restoreBarIndex ?: -1
            if (expectedBar > 0 && requestedBarIndex > 0 && requestedBarIndex != expectedBar) {
                // Ignore callbacks from internal re-renders (scale/mode) that are not our restore target.
                Log.d(
                    RESTORE_TAG,
                    "onRestoreApplied ignored (requestedBar=$requestedBarIndex expectedBar=$expectedBar tick=$tick currentBar=$currentBarIndex)"
                )
                return@update state
            }
            val targetBar = if (expectedBar > 0) expectedBar else requestedBarIndex
            val reached = when {
                targetBar > 1 -> tick > 0L && abs(currentBarIndex - targetBar) <= 1
                else -> tick > 0L
            }
            Log.d(
                RESTORE_TAG,
                "onRestoreApplied tick=$tick currentBar=$currentBarIndex requestedBar=$requestedBarIndex targetBar=$targetBar reached=$reached"
            )
            if (!reached) return@update state
            state.copy(
                restorePending = false,
                lastTickPosition = tick,
                lastBarIndex = if (currentBarIndex > 0) currentBarIndex else state.lastBarIndex
            )
        }
    }

    fun updatePlaybackProgress(
        lessonId: String,
        lessonTitle: String,
        tick: Long,
        barIndex: Int,
        totalBars: Int
    ) {
        val currentState = _uiState.value
        if (currentState.restorePending) {
            Log.d(
                RESTORE_TAG,
                "updatePlaybackProgress skipped: restorePending tick=$tick barIndex=$barIndex"
            )
            return
        }
        if (tick <= 0L || barIndex <= 0 || totalBars <= 0) {
            if (tick > 0L) {
                Log.d(RESTORE_TAG, "updatePlaybackProgress skipped: tick=$tick barIndex=$barIndex totalBars=$totalBars")
            }
            return
        }
        val now = System.currentTimeMillis()
        val shouldUpdate = barIndex != lastSavedBarIndex || now - lastSavedAt > 2000L
        if (!shouldUpdate || totalBars <= 0 || barIndex <= 0) return
        lastSavedBarIndex = barIndex
        lastSavedAt = now
        _uiState.update {
            it.copy(
                lastTickPosition = tick,
                lastBarIndex = barIndex,
                totalBars = totalBars
            )
        }
        viewModelScope.launch {
            Log.d(
                RESTORE_TAG,
                "persist progress: lessonId=$lessonId tick=$tick bar=$barIndex total=$totalBars"
            )
            updateTabPlaybackProgressUseCase(
                TabPlaybackProgress(
                    tabId = lessonId,
                    tabName = lessonTitle,
                    lastTick = tick,
                    lastBarIndex = barIndex,
                    totalBars = totalBars,
                    updatedAt = now
                )
            )
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
