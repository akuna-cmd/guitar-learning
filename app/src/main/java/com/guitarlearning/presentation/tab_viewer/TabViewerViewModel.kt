package com.guitarlearning.presentation.tab_viewer

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guitarlearning.BuildConfig
import com.guitarlearning.di.AppDispatchers
import com.guitarlearning.domain.model.Lesson
import com.guitarlearning.domain.model.TabPlaybackProgress
import com.guitarlearning.domain.repository.SoundFontRepository
import com.guitarlearning.domain.repository.TabFileRepository
import com.guitarlearning.domain.repository.TabPlaybackProgressRepository
import com.guitarlearning.domain.repository.TabRepository
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
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
    val isDead: Boolean = false,
    val isHammer: Boolean = false,
    val isPullOff: Boolean = false,
    val isSlide: Boolean = false,
    val isVibrato: Boolean = false,
    val isGhost: Boolean = false,
    val hasBend: Boolean = false,
    val isPalmMute: Boolean = false,
    val hasHarmonic: Boolean = false,
    val isTrill: Boolean = false,
    val isLetRing: Boolean = false,
    val isSlap: Boolean = false,
    val isPop: Boolean = false,
    val isAccent: Boolean = false,
    val isTapping: Boolean = false
)

@Immutable
data class TabAnalysis(
    val barIndex: Int,
    val leftHand: List<FingerInfo>,
    val rightHand: List<FingerInfo>,
    val instructions: List<String>,
    val barreFret: Int? = null,
    val nextLeftHand: List<FingerInfo> = emptyList(),
    val contextHint: String? = null
)

data class TabViewerUiState(
    val lesson: Lesson? = null,
    val isUserTab: Boolean = false,
    val tabs: List<LessonTab> = LessonTab.values().toList(),
    val selectedTab: LessonTab = LessonTab.TABS,
    val selectedTabIndex: Int = LessonTab.values().indexOf(LessonTab.TABS),
    val asciiTab: String? = null,
    val compactTabs: String? = null,
    val tabAnalysis: TabAnalysis? = null,
    val tabBytesReady: Boolean = false,
    val tabBytesPath: String? = null,
    val soundFontReady: Boolean = false,
    val totalBars: Int? = null,
    val restoreTickPosition: Long? = null,
    val restoreBarIndex: Int? = null,
    val restorePending: Boolean = false,
    val isScoreLoaded: Boolean = false
)

@HiltViewModel
class TabViewerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tabRepository: TabRepository,
    private val tabFileRepository: TabFileRepository,
    private val soundFontRepository: SoundFontRepository,
    private val tabPlaybackProgressRepository: TabPlaybackProgressRepository,
    private val dispatchers: AppDispatchers
) : ViewModel() {

    private companion object {
        const val RESTORE_TAG = "TabRestoreFlow"
        const val LOAD_TAG = "TabLoadPerf"
        const val ENABLE_PERF_LOGS = false
        private val tabBase64Cache = LinkedHashMap<String, String>(32, 0.75f, true)
        private var soundFontBase64Cache: String? = null
    }

    private val gson = Gson()
    private val _uiState = MutableStateFlow(TabViewerUiState())
    val uiState: StateFlow<TabViewerUiState> = _uiState.asStateFlow()

    private val _lastTickPosition = MutableStateFlow<Long?>(null)
    val lastTickPosition = _lastTickPosition.asStateFlow()

    private val _lastBarIndex = MutableStateFlow<Int?>(null)
    val lastBarIndex = _lastBarIndex.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private var lastSavedBarIndex: Int = -1
    private var activeLessonId: String? = null

    @Volatile
    var tabBytesRef: String? = null
        private set

    @Volatile
    var soundFontRef: String? = null
        private set

    init {
        loadSoundFont()
    }

    fun loadLesson(id: String) {
        val soundFontAlreadyReady = _uiState.value.soundFontReady
        _uiState.value = TabViewerUiState(soundFontReady = soundFontAlreadyReady)
        _lastTickPosition.value = null
        _lastBarIndex.value = null

        viewModelScope.launch(dispatchers.io) {
            tabRepository.markTabOpened(id)
        }

        viewModelScope.launch {
            val (lesson, tabItem, savedProgress) = withContext(dispatchers.io) {
                coroutineScope {
                    val lessonDeferred = async { tabRepository.getLesson(id) }
                    val tabItemDeferred = async { tabRepository.getTabById(id) }
                    val progressDeferred = async { tabPlaybackProgressRepository.getByTabId(id) }
                    Triple(
                        lessonDeferred.await(),
                        tabItemDeferred.await(),
                        progressDeferred.await()
                    )
                }
            }

            val savedBar = savedProgress?.lastBarIndex ?: 0
            val savedTick = savedProgress?.lastTick ?: 0L
            val shouldRestore = savedBar > 1 || savedTick > 1L

            activeLessonId = id
            lastSavedBarIndex = savedBar.takeIf { it > 0 } ?: -1

            perfLog(
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

            val tabPath = lesson?.tabsGpPath ?: tabItem?.filePath
            if (tabPath != null) {
                loadTabBytes(tabPath)
            }
        }
    }

    fun markScoreLoading() {
        _uiState.update { state ->
            if (!state.isScoreLoaded) state else state.copy(isScoreLoaded = false)
        }
    }

    fun markScoreLoaded(totalBars: Int) {
        if (totalBars <= 0) return
        _uiState.update { state ->
            state.copy(
                isScoreLoaded = true,
                totalBars = totalBars
            )
        }
    }

    fun loadTabBytes(path: String) {
        if (_uiState.value.tabBytesPath == path) return
        val startedAt = System.currentTimeMillis()
        val cached = synchronized(tabBase64Cache) { tabBase64Cache[path] }
        if (cached != null) {
            perfLog(
                LOAD_TAG,
                "tabBytes memory cache HIT path=$path len=${cached.length} took=${System.currentTimeMillis() - startedAt}ms"
            )
            tabBytesRef = cached
            _uiState.update { it.copy(tabBytesReady = true, tabBytesPath = path) }
            return
        }

        viewModelScope.launch(dispatchers.io) {
            val diskCached = readTabBase64FromDisk(path)
            if (diskCached != null) {
                synchronized(tabBase64Cache) {
                    tabBase64Cache[path] = diskCached
                }
                perfLog(
                    LOAD_TAG,
                    "tabBytes disk cache HIT path=$path len=${diskCached.length} took=${System.currentTimeMillis() - startedAt}ms"
                )
                tabBytesRef = diskCached
                _uiState.update { it.copy(tabBytesReady = true, tabBytesPath = path) }
                return@launch
            }

            val sourceStart = System.currentTimeMillis()
            val base64 = runCatching {
                Base64.encodeToString(tabFileRepository.readTabBytes(path), Base64.NO_WRAP)
            }.getOrNull()

            perfLog(
                LOAD_TAG,
                "tabBytes source load path=$path base64Len=${base64?.length ?: 0} sourceMs=${System.currentTimeMillis() - sourceStart} totalMs=${System.currentTimeMillis() - startedAt}"
            )

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

    fun setAsciiTab(ascii: String) {
        _uiState.update { it.copy(asciiTab = ascii) }
    }

    fun setCompactTabs(tabs: String) {
        _uiState.update { it.copy(compactTabs = tabs) }
    }

    fun setTabAnalysis(analysisJson: String) {
        runCatching {
            gson.fromJson(analysisJson, TabAnalysis::class.java)
        }.onSuccess { analysis ->
            _uiState.update { it.copy(tabAnalysis = analysis) }
        }.onFailure { error ->
            Log.e("TabViewerViewModel", "Error parsing analysis", error)
        }
    }

    fun updatePlaybackState(tick: Long, isPlaying: Boolean) {
        _lastTickPosition.value = tick
        _isPlaying.value = isPlaying
    }

    fun markRestoreCompleted() {
        perfLog(RESTORE_TAG, "markRestoreCompleted()")
        _uiState.update { state ->
            if (!state.restorePending) state else state.copy(restorePending = false)
        }
    }

    fun onRestoreApplied(tick: Long, currentBarIndex: Int, requestedBarIndex: Int) {
        _uiState.update { state ->
            if (!state.restorePending) return@update state
            val expectedBar = state.restoreBarIndex ?: -1
            if (expectedBar > 0 && requestedBarIndex > 0 && requestedBarIndex != expectedBar) {
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
            _lastTickPosition.value = tick
            _lastBarIndex.value = if (currentBarIndex > 0) currentBarIndex else _lastBarIndex.value
            state.copy(restorePending = false)
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
        if (activeLessonId != lessonId) return
        if (currentState.restorePending) return

        if (tick <= 0L || barIndex <= 0 || totalBars <= 0) return
        if (barIndex == lastSavedBarIndex) return

        val now = System.currentTimeMillis()
        lastSavedBarIndex = barIndex
        _lastTickPosition.value = tick
        _lastBarIndex.value = barIndex
        _uiState.update { it.copy(totalBars = totalBars) }

        viewModelScope.launch(dispatchers.io) {
            tabPlaybackProgressRepository.upsert(
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

    private fun loadSoundFont() {
        if (_uiState.value.soundFontReady) return
        val startedAt = System.currentTimeMillis()
        soundFontBase64Cache?.let { cached ->
            perfLog(
                LOAD_TAG,
                "soundFont memory cache HIT len=${cached.length} took=${System.currentTimeMillis() - startedAt}ms"
            )
            soundFontRef = cached
            _uiState.update { it.copy(soundFontReady = true) }
            return
        }

        viewModelScope.launch(dispatchers.io) {
            val diskCached = readSoundFontBase64FromDisk()
            if (diskCached != null) {
                soundFontBase64Cache = diskCached
                perfLog(
                    LOAD_TAG,
                    "soundFont disk cache HIT len=${diskCached.length} took=${System.currentTimeMillis() - startedAt}ms"
                )
                soundFontRef = diskCached
                _uiState.update { it.copy(soundFontReady = true) }
                return@launch
            }

            val sourceStart = System.currentTimeMillis()
            val base64 = runCatching {
                Base64.encodeToString(soundFontRepository.readSoundFontBytes(), Base64.NO_WRAP)
            }.getOrNull()

            perfLog(
                LOAD_TAG,
                "soundFont source load base64Len=${base64?.length ?: 0} sourceMs=${System.currentTimeMillis() - sourceStart} totalMs=${System.currentTimeMillis() - startedAt}"
            )

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

    private fun perfLog(tag: String, message: String) {
        if (BuildConfig.DEBUG && ENABLE_PERF_LOGS) {
            Log.d(tag, message)
        }
    }
}
