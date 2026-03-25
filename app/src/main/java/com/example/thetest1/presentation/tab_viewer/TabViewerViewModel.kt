package com.example.thetest1.presentation.tab_viewer

import android.content.Context
import android.net.Uri
import android.util.Log
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.Immutable
import com.example.thetest1.BuildConfig
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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.security.MessageDigest
import java.util.Date
import kotlin.math.abs

enum class LessonTab {
    THEORY,
    TABS,
    AI_ASSISTANT,
    NOTES
}

@Immutable
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

@Immutable
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
    // Lightweight flags — actual bytes are held in ViewModel fields to avoid Compose recompositions
    val tabBytesReady: Boolean = false,
    val tabBytesPath: String? = null,
    val soundFontReady: Boolean = false,
    val totalBars: Int? = null,
    val restoreTickPosition: Long? = null,
    val restoreBarIndex: Int? = null,
    val restorePending: Boolean = false,
    val isScoreLoaded: Boolean = false
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
        const val LOAD_TAG = "TabLoadPerf"
        const val ENABLE_PERF_LOGS = false
        // Process-wide lightweight cache for already opened tabs/soundfont.
        private val tabBase64Cache = LinkedHashMap<String, String>(32, 0.75f, true)
        private var soundFontBase64Cache: String? = null
    }

    private fun perfLog(tag: String, message: String) {
        if (BuildConfig.DEBUG && ENABLE_PERF_LOGS) {
            Log.d(tag, message)
        }
    }

    private val _uiState = MutableStateFlow(TabViewerUiState())
    val uiState: StateFlow<TabViewerUiState> = _uiState.asStateFlow()

    private val _lastTickPosition = MutableStateFlow<Long?>(null)
    val lastTickPosition = _lastTickPosition.asStateFlow()

    private val _lastBarIndex = MutableStateFlow<Int?>(null)
    val lastBarIndex = _lastBarIndex.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()


    private val audioRecorder by lazy { AudioRecorder(context) }
    private val audioPlayer by lazy { AudioPlayer(context) }
    private var audioFile: File? = null
    private var audioNotesJob: Job? = null
    private var textNotesJob: Job? = null
    private val gson = Gson()
    private var lastSavedBarIndex: Int = -1
    private var lastSavedAt: Long = 0L
    private var activeLessonId: String? = null
    private var lessonOpenedAt: Long = 0L
    private var restoreFloorBarIndex: Int = 0

    init {
        viewModelScope.launch {
            audioPlayer.playerState.collect { playerState ->
                _uiState.update { it.copy(playerState = playerState) }
            }
        }
        loadSoundFont()
    }

    // Direct references — not flowed through Compose state to avoid giant recompositions
    @Volatile var tabBytesRef: String? = null
        private set
    @Volatile var soundFontRef: String? = null
        private set

    fun loadLesson(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Parallelize all 3 DB queries
            val (lesson, tabItem, savedProgress) = coroutineScope {
                val l = async { getLessonUseCase(id) }
                val t = async { getTabItemUseCase(id) }
                val p = async { getTabPlaybackProgressUseCase(id) }
                Triple(l.await(), t.await(), p.await())
            }
            val savedBar = savedProgress?.lastBarIndex ?: 0
            val savedTick = savedProgress?.lastTick ?: 0L
            val shouldRestore = savedBar > 1 || savedTick > 1L
            activeLessonId = id
            lessonOpenedAt = System.currentTimeMillis()
            restoreFloorBarIndex = savedBar.coerceAtLeast(0)
            lastSavedBarIndex = savedBar.takeIf { it > 0 } ?: -1
            lastSavedAt = 0L
            perfLog(
                RESTORE_TAG,
                "loadLesson(id=$id) savedProgress={tick=${savedProgress?.lastTick}, bar=${savedProgress?.lastBarIndex}, total=${savedProgress?.totalBars}} shouldRestore=$shouldRestore restoreFloor=$restoreFloorBarIndex"
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

                _lastTickPosition.value = savedProgress?.lastTick
                _lastBarIndex.value = savedProgress?.lastBarIndex

                currentState.copy(
                    lesson = lesson,
                    isUserTab = isUserTab,
                    tabs = tabs,
                    selectedTab = selectedTab,
                    selectedTabIndex = if (selectedTabIndex != -1) selectedTabIndex else 0,
                    totalBars = savedProgress?.totalBars,
                    restoreTickPosition = savedProgress?.lastTick,
                    restoreBarIndex = savedProgress?.lastBarIndex,
                    restorePending = shouldRestore
                )
            }
            // Launch tab bytes and audio/text notes loading in parallel (don't wait for lesson)
            val tabPath = lesson?.tabsGpPath ?: tabItem?.filePath
            if (tabPath != null) {
                launch { loadTabBytes(tabPath) }
            }
            lesson?.let {
                audioNotesJob?.cancel()
                audioNotesJob = launch {
                    getAudioNotesUseCase(it.id).collect { audioNotes ->
                        _uiState.update { it.copy(audioNotes = audioNotes) }
                    }
                }
                textNotesJob?.cancel()
                textNotesJob = launch {
                    getTextNotesUseCase(it.id).collect { textNotes ->
                        _uiState.update { it.copy(textNotes = textNotes) }
                    }
                }
            }
        }
    }

    fun loadTabBytes(path: String) {
        if (_uiState.value.tabBytesPath == path) return
        val t0 = System.currentTimeMillis()
        val cached = synchronized(tabBase64Cache) { tabBase64Cache[path] }
        if (cached != null) {
            perfLog(LOAD_TAG, "tabBytes memory cache HIT path=$path len=${cached.length} took=${System.currentTimeMillis()-t0}ms")
            tabBytesRef = cached
            _uiState.update { it.copy(tabBytesReady = true, tabBytesPath = path) }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val diskCached = readTabBase64FromDisk(path)
            if (diskCached != null) {
                synchronized(tabBase64Cache) {
                    tabBase64Cache[path] = diskCached
                }
                perfLog(LOAD_TAG, "tabBytes disk cache HIT path=$path len=${diskCached.length} took=${System.currentTimeMillis()-t0}ms")
                tabBytesRef = diskCached
                _uiState.update { it.copy(tabBytesReady = true, tabBytesPath = path) }
                return@launch
            }
            val sourceStart = System.currentTimeMillis()
            val base64 = getTabFileBytesUseCase(path).getOrNull()?.let { bytes ->
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            }
            perfLog(LOAD_TAG, "tabBytes source load path=$path base64Len=${base64?.length ?: 0} sourceMs=${System.currentTimeMillis()-sourceStart} totalMs=${System.currentTimeMillis()-t0}")
            if (base64 != null) {
                synchronized(tabBase64Cache) {
                    tabBase64Cache[path] = base64
                    while (tabBase64Cache.size > 40) {
                        val firstKey = tabBase64Cache.entries.firstOrNull()?.key ?: break
                        tabBase64Cache.remove(firstKey)
                    }
                }
                writeTabBase64ToDisk(path, base64)
                tabBytesRef = base64
                _uiState.update { it.copy(tabBytesReady = true, tabBytesPath = path) }
            } else {
                _uiState.update { it.copy(tabBytesReady = false, tabBytesPath = path) }
            }
        }
    }

    private fun loadSoundFont() {
        if (_uiState.value.soundFontReady) return
        val t0 = System.currentTimeMillis()
        soundFontBase64Cache?.let { cached ->
            perfLog(LOAD_TAG, "soundFont memory cache HIT len=${cached.length} took=${System.currentTimeMillis()-t0}ms")
            soundFontRef = cached
            _uiState.update { it.copy(soundFontReady = true) }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val diskCached = readSoundFontBase64FromDisk()
            if (diskCached != null) {
                soundFontBase64Cache = diskCached
                perfLog(LOAD_TAG, "soundFont disk cache HIT len=${diskCached.length} took=${System.currentTimeMillis()-t0}ms")
                soundFontRef = diskCached
                _uiState.update { it.copy(soundFontReady = true) }
                return@launch
            }
            val sourceStart = System.currentTimeMillis()
            val base64 = getSoundFontBytesUseCase().getOrNull()?.let { bytes ->
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            }
            perfLog(LOAD_TAG, "soundFont source load base64Len=${base64?.length ?: 0} sourceMs=${System.currentTimeMillis()-sourceStart} totalMs=${System.currentTimeMillis()-t0}")
            if (base64 != null) {
                soundFontBase64Cache = base64
                writeSoundFontBase64ToDisk(base64)
                soundFontRef = base64
                _uiState.update { it.copy(soundFontReady = true) }
            }
        }
    }

    private fun cacheRootDir(): File {
        return File(context.cacheDir, "tab_b64_cache").apply { mkdirs() }
    }

    private fun pathHash(path: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(path.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { b -> "%02x".format(b) }
    }

    private fun tabCacheFile(path: String): File {
        return File(cacheRootDir(), "tab_${pathHash(path)}.b64")
    }

    private fun soundFontCacheFile(): File {
        return File(cacheRootDir(), "soundfont.b64")
    }

    private fun readTabBase64FromDisk(path: String): String? {
        return try {
            val file = tabCacheFile(path)
            if (!file.exists()) null else file.readText()
        } catch (_: Exception) {
            null
        }
    }

    private fun writeTabBase64ToDisk(path: String, value: String) {
        runCatching {
            tabCacheFile(path).writeText(value)
        }
    }

    private fun readSoundFontBase64FromDisk(): String? {
        return try {
            val file = soundFontCacheFile()
            if (!file.exists()) null else file.readText()
        } catch (_: Exception) {
            null
        }
    }

    private fun writeSoundFontBase64ToDisk(value: String) {
        runCatching {
            soundFontCacheFile().writeText(value)
        }
    }

    fun setAsciiTab(ascii: String) {
        _uiState.update { it.copy(asciiTab = ascii) }
    }

    fun setCompactTabs(tabs: String) {
        _uiState.update { it.copy(compactTabs = tabs) }
    }

    fun setTabAnalysis(analysisJson: String) {
        try {
            val analysis = gson.fromJson(analysisJson, TabAnalysis::class.java)
            _uiState.update { it.copy(tabAnalysis = analysis) }
        } catch (e: Exception) {
            Log.e("TabViewerViewModel", "Error parsing analysis", e)
        }
    }

    fun updatePlaybackState(tick: Long, isPlaying: Boolean) {
        _lastTickPosition.value = tick
        _isPlaying.value = isPlaying
    }

    fun markRestoreCompleted() {
        perfLog(RESTORE_TAG, "markRestoreCompleted()")
        restoreFloorBarIndex = 0
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
                perfLog(
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
            perfLog(
                RESTORE_TAG,
                "onRestoreApplied tick=$tick currentBar=$currentBarIndex requestedBar=$requestedBarIndex targetBar=$targetBar reached=$reached"
            )
            if (!reached) return@update state
            restoreFloorBarIndex = 0
            _lastTickPosition.value = tick
            _lastBarIndex.value = if (currentBarIndex > 0) currentBarIndex else _lastBarIndex.value
            state.copy(
                restorePending = false,
            )
        }
    }

    fun updatePlaybackProgress(
        lessonId: String,
        lessonTitle: String,
        tick: Long,
        isPlaying: Boolean,
        barIndex: Int,
        totalBars: Int
    ) {
        val currentState = _uiState.value
        if (activeLessonId != lessonId) {
            return
        }
        if (currentState.restorePending) {
            return
        }
        val openedAgoMs = System.currentTimeMillis() - lessonOpenedAt
        val floor = restoreFloorBarIndex
        if (floor > 1 && openedAgoMs < 7000L && barIndex < floor) {
            return
        }
        if (tick <= 0L || barIndex <= 0 || totalBars <= 0) {
            return
        }
        if (!isPlaying && barIndex < lastSavedBarIndex && openedAgoMs < 15000L) {
            return
        }
        val now = System.currentTimeMillis()
        val shouldUpdate = barIndex != lastSavedBarIndex
        if (!shouldUpdate || totalBars <= 0 || barIndex <= 0) return
        lastSavedBarIndex = barIndex
        lastSavedAt = now
        _lastTickPosition.value = tick
        _lastBarIndex.value = barIndex
        _uiState.update {
            it.copy(
                totalBars = totalBars
            )
        }
        viewModelScope.launch {
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
