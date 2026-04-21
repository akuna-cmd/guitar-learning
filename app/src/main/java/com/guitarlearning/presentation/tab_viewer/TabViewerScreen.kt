package com.guitarlearning.presentation.tab_viewer

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guitarlearning.R
import com.guitarlearning.BuildConfig
import com.guitarlearning.presentation.ai_assistant.AiAssistantScreen
import com.guitarlearning.presentation.main.MainViewModel
import com.guitarlearning.presentation.notes.NotesScreen
import com.guitarlearning.presentation.main.ThemeViewModel
import com.guitarlearning.presentation.main.TabDisplayMode
import com.guitarlearning.presentation.main.FretboardDisplayMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabViewerScreen(
    lessonId: String,
    onBack: () -> Unit
) {
    val activity = LocalContext.current as ComponentActivity
    val viewModel: TabViewerViewModel = hiltViewModel()
    val notesViewModel: TabNotesViewModel = hiltViewModel()
    val mainViewModel: MainViewModel = hiltViewModel(activity)
    val themeViewModel: ThemeViewModel = hiltViewModel(activity)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val notesUiState by notesViewModel.uiState.collectAsStateWithLifecycle()
    val lastTickPosition by viewModel.lastTickPosition.collectAsStateWithLifecycle()
    val lastBarIndex by viewModel.lastBarIndex.collectAsStateWithLifecycle()
    val isPlayingState by viewModel.isPlaying.collectAsStateWithLifecycle()

    val themeUiState by themeViewModel.uiState.collectAsStateWithLifecycle()
    val lesson = uiState.lesson?.takeIf { it.id == lessonId }
    val rawDisplayReady = lesson != null && uiState.isScoreLoaded
    var isDisplayUnlocked by remember(lessonId) { mutableStateOf(false) }

    LaunchedEffect(lessonId) {
        isDisplayUnlocked = false
    }

    LaunchedEffect(rawDisplayReady, lessonId) {
        if (!rawDisplayReady) {
            isDisplayUnlocked = false
            return@LaunchedEffect
        }
        delay(180)
        if (rawDisplayReady) {
            isDisplayUnlocked = true
        }
    }

    BackHandler {
        onBack()
    }

    LaunchedEffect(lessonId) {
        viewModel.loadLesson(lessonId)
        notesViewModel.bindLesson(lessonId)
    }

    lesson?.let {
        LaunchedEffect(it) {
            mainViewModel.setActiveTab(it.id, it.title)
        }
    }

    LaunchedEffect(isDisplayUnlocked, lessonId) {
        if (isDisplayUnlocked) {
            TabLoadMetricsTracker.markFullyVisible(lessonId)
        }
    }

    var showAiSheet by remember { mutableStateOf(false) }
    var showNotesSheet by remember { mutableStateOf(false) }
    var showLoopSheet by remember { mutableStateOf(false) }
    var totalMeasures by remember { mutableStateOf(1) }
    var loopStartMeasure by remember { mutableStateOf(1) }
    var loopEndMeasure by remember { mutableStateOf(1) }
    var isLoopEnabled by remember { mutableStateOf(false) }
    var silentMode by remember { mutableStateOf(false) }
    val aiSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val notesSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val loopSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val tabScaleOverrides = remember { mutableStateMapOf<String, Float>() }

    val contentAlpha = if (isDisplayUnlocked) 1f else 0f

    Scaffold(
        modifier = Modifier,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { 
                        onBack() 
                    }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back_arrow))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (lesson != null) {
                var isPracticeMode by remember { mutableStateOf(false) }
                var currentSpeed by remember(isPracticeMode) {
                    mutableStateOf(if (isPracticeMode) themeUiState.practiceSpeed else themeUiState.normalSpeed)
                }
                val defaultScale = if (isPracticeMode) themeUiState.practiceTabScale else themeUiState.normalTabScale
                var currentScale by remember(lesson.id, isPracticeMode, defaultScale) {
                    mutableStateOf(tabScaleOverrides[lesson.id] ?: defaultScale)
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(contentAlpha)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val modes = listOf(
                            false to stringResource(R.string.settings_mode_normal),
                            true to stringResource(R.string.settings_mode_practice)
                        )
                        modes.forEach { (practice, label) ->
                            val selected = isPracticeMode == practice
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { isPracticeMode = practice }
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = if (practice) Icons.Filled.Tune else Icons.Filled.SportsEsports,
                                    contentDescription = stringResource(
                                        if (practice) R.string.mode_practice_icon_desc else R.string.mode_normal_icon_desc
                                    ),
                                    tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = label,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    TabViewer(
                        model = TabViewerModel(
                            tabId = lesson.id,
                            fileName = lesson.tabsGpPath,
                            tabBytesReady = uiState.tabBytesReady,
                            soundFontReady = uiState.soundFontReady,
                            tabTitle = lesson.title,
                            isPracticeMode = isPracticeMode,
                            currentSpeed = currentSpeed,
                            tabDisplayMode = themeUiState.tabDisplayMode,
                            lastTickPosition = lastTickPosition,
                            lastBarIndex = lastBarIndex,
                            restoreTickPosition = uiState.restoreTickPosition,
                            restoreBarIndex = uiState.restoreBarIndex,
                            totalBars = uiState.totalBars,
                            restorePending = uiState.restorePending,
                            wasPlaying = isPlayingState,
                            currentScale = currentScale,
                            silentMode = silentMode,
                            themeUiState = themeUiState,
                            isPlaying = isPlayingState,
                            loopStartMeasure = loopStartMeasure,
                            loopEndMeasure = loopEndMeasure,
                            isLoopEnabled = isLoopEnabled
                        ),
                        handlers = TabViewerHandlers(
                            onSpeedChange = { currentSpeed = it },
                            onTabDisplayModeChange = { mode -> themeViewModel.setTabDisplayMode(mode) },
                            onRestoreApplied = { tick, currentBar, requestedBar ->
                                viewModel.onRestoreApplied(tick, currentBar, requestedBar)
                            },
                            onTickPosition = { tick, playing ->
                                viewModel.updatePlaybackState(tick, playing)
                            },
                            onPlaybackProgress = { tick, playing, barIndex, bars ->
                                viewModel.updatePlaybackState(tick, playing)
                                viewModel.markScoreLoaded(bars)
                                viewModel.updatePlaybackProgress(
                                    lessonId = lesson.id,
                                    lessonTitle = lesson.title,
                                    tick = tick,
                                    isPlaying = playing,
                                    barIndex = barIndex,
                                    totalBars = bars
                                )
                            },
                            onScaleChange = { scale ->
                                currentScale = scale
                                tabScaleOverrides[lesson.id] = scale
                            },
                            onSilentModeChange = { silentMode = it },
                            onOpenAiAssistant = { showAiSheet = true },
                            onOpenNotes = { showNotesSheet = true },
                            onOpenLoop = { showLoopSheet = true },
                            onPlayStateChange = { viewModel.updatePlaybackState(lastTickPosition ?: 0L, it) },
                            onAsciiTabGenerated = { ascii -> viewModel.setAsciiTab(ascii) },
                            onTabAnalysis = { analysis -> viewModel.setTabAnalysis(analysis) },
                            onCompactTabsGenerated = { tabs -> viewModel.setCompactTabs(tabs) },
                            onTotalMeasuresLoaded = { measures ->
                                totalMeasures = measures
                                viewModel.markScoreLoaded(measures)
                                if (loopEndMeasure == 1 && measures > 1) {
                                    loopEndMeasure = measures
                                }
                            }
                        ),
                        tabViewModel = viewModel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(top = 16.dp, start = 8.dp, end = 8.dp)
                    )

                    androidx.compose.animation.AnimatedVisibility(visible = isPracticeMode && uiState.isScoreLoaded) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp)
                        ) {
                            GuitarFretboard(
                                analysis = uiState.tabAnalysis,
                                isPlaying = isPlayingState,
                                displayMode = themeUiState.fretboardDisplayMode,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp)
                            )
                        }
                    }

                }

                if (showAiSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showAiSheet = false },
                        sheetState = aiSheetState,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight(0.95f)
                                .imePadding()
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            AiAssistantScreen(
                                lesson = lesson,
                                asciiTab = uiState.asciiTab,
                                compactTabs = uiState.compactTabs,
                                totalMeasures = totalMeasures,
                                initialMeasureRange = if (isLoopEnabled) {
                                    loopStartMeasure..loopEndMeasure
                                } else {
                                    lastBarIndex?.takeIf { it > 0 }?.let { it..it } ?: (1..1)
                                }
                            )
                        }
                    }
                }

                if (showNotesSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showNotesSheet = false },
                        sheetState = notesSheetState
                    ) {
                        Box(modifier = Modifier.fillMaxHeight(0.95f).imePadding()) {
                            NotesScreen(
                                audioNotes = notesUiState.audioNotes,
                                textNotes = notesUiState.textNotes,
                                isRecording = notesUiState.isRecording,
                                playerState = notesUiState.playerState,
                                onAddAudioNote = { uri -> notesViewModel.addAudioNoteFromFile(lesson.id, uri) },
                                onRecordAudio = { notesViewModel.onRecordAudio(lesson.id) },
                                onDeleteAudioNote = { id -> notesViewModel.deleteAudioNote(id) },
                                onPlayAudio = { note -> notesViewModel.onPlayAudio(note) },
                                onSeekAudio = { id, prog -> notesViewModel.onSeekAudio(id, prog) },
                                onAddTextNote = { content -> notesViewModel.addTextNote(lesson.id, content) },
                                onUpdateTextNote = { note -> notesViewModel.updateTextNote(note) },
                                onDeleteTextNote = { note -> notesViewModel.deleteTextNote(note) }
                            )
                        }
                    }
                }

                if (showLoopSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showLoopSheet = false },
                        sheetState = loopSheetState
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().imePadding()) {
                            LoopConfigurator(
                                totalMeasures = totalMeasures,
                                startMeasure = loopStartMeasure,
                                endMeasure = loopEndMeasure,
                                isLoopEnabled = isLoopEnabled,
                                onStartChange = { loopStartMeasure = it },
                                onEndChange = { loopEndMeasure = it },
                                onToggleLoop = { isLoopEnabled = it }
                            )
                        }
                    }
                }
            }

            if (lesson == null || !isDisplayUnlocked) {
                TabViewerLoadingScreen(
                    modifier = Modifier.matchParentSize()
                )
            }
        }
    }
}

@Composable
private fun TabViewerLoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun TabViewer(
    model: TabViewerModel,
    handlers: TabViewerHandlers,
    tabViewModel: TabViewerViewModel,
    modifier: Modifier = Modifier
) {
    val tabId = model.tabId
    val fileName = model.fileName
    val tabBytesReady = model.tabBytesReady
    val soundFontReady = model.soundFontReady
    val tabTitle = model.tabTitle
    val isPracticeMode = model.isPracticeMode
    val currentSpeed = model.currentSpeed
    val tabDisplayMode = model.tabDisplayMode
    val lastTickPosition = model.lastTickPosition
    val lastBarIndex = model.lastBarIndex
    val restoreTickPosition = model.restoreTickPosition
    val restoreBarIndex = model.restoreBarIndex
    val initialTotalBars = model.totalBars ?: 0
    val restorePending = model.restorePending
    val wasPlaying = model.wasPlaying
    val currentScale = model.currentScale
    val silentMode = model.silentMode
    val themeUiState = model.themeUiState
    val isPlaying = model.isPlaying
    val loopStartMeasure = model.loopStartMeasure
    val loopEndMeasure = model.loopEndMeasure
    val isLoopEnabled = model.isLoopEnabled

    val onSpeedChange = handlers.onSpeedChange
    val onTabDisplayModeChange = handlers.onTabDisplayModeChange
    val onRestoreApplied = handlers.onRestoreApplied
    val onTickPosition = handlers.onTickPosition
    val onPlaybackProgress = handlers.onPlaybackProgress
    val onScaleChange = handlers.onScaleChange
    val onSilentModeChange = handlers.onSilentModeChange
    val onOpenAiAssistant = handlers.onOpenAiAssistant
    val onOpenNotes = handlers.onOpenNotes
    val onOpenLoop = handlers.onOpenLoop
    val onPlayStateChange = handlers.onPlayStateChange
    val onAsciiTabGenerated = handlers.onAsciiTabGenerated
    val onTabAnalysis = handlers.onTabAnalysis
    val onCompactTabsGenerated = handlers.onCompactTabsGenerated
    val onTotalMeasuresLoaded = handlers.onTotalMeasuresLoaded

    val restoreTag = "TabRestoreFlow"
    val loadTag = "TabLoadPerf"
    val layoutTag = "TabLayoutTrace"
    val frameTag = "TabFramePerf"
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeUiState.themeMode) {
        com.guitarlearning.presentation.main.ThemeMode.DARK -> true
        com.guitarlearning.presentation.main.ThemeMode.LIGHT -> false
        com.guitarlearning.presentation.main.ThemeMode.SYSTEM -> isSystemDark
    }
    val languageTag = themeUiState.appLanguage.languageTag
    val jsLanguageTag = remember(languageTag) {
        languageTag.replace("\\", "\\\\").replace("'", "\\'")
    }
    val webEntry = remember(fileName) {
        createTabWebViewEntry(context)
    }
    var isReady by remember(fileName) { mutableStateOf(webEntry.jsReady) }
    var isScoreLoaded by remember(fileName) { mutableStateOf(false) }
    var showLoadingOverlay by remember(fileName) { mutableStateOf(true) }
    var totalBars by remember(fileName) { mutableStateOf(initialTotalBars) }
    var loadedSourceForCurrentLesson by remember(fileName) { mutableStateOf<String?>(null) }
    var loadRequestAtMs by remember { mutableStateOf(0L) }
    var jsReadyAtMs by remember { mutableStateOf(0L) }
    var showDisplaySheet by remember { mutableStateOf(false) }
    var showLearningSheet by remember { mutableStateOf(false) }
    var metronomeEnabled by remember { mutableStateOf(false) }
    var metronomeBpm by remember { mutableStateOf(90) }
    var metronomeBpmTouched by remember { mutableStateOf(false) }
    val displaySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val learningSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var lastControlsY by remember { mutableStateOf<Int?>(null) }
    var lastWebY by remember { mutableStateOf<Int?>(null) }
    
    val webView = webEntry.webView

    LaunchedEffect(webView) {
        runCatching {
            webView.onResume()
        }
    }

    LaunchedEffect(fileName, webView) {
        if (isReady) return@LaunchedEffect
        repeat(20) {
            if (isReady || webEntry.jsReady) {
                isReady = true
                return@LaunchedEffect
            }
            runCatching {
                webView.evaluateJavascript("(function(){return window.__tabViewerReady===true;})()") { result ->
                    if (result == "true") {
                        webEntry.jsReady = true
                        isReady = true
                    }
                }
            }
            delay(150)
        }
    }

    val bridge = webEntry.bridge

    DisposableEffect(
        bridge,
        onAsciiTabGenerated,
        onTabAnalysis,
        onCompactTabsGenerated,
        onTickPosition,
        onPlaybackProgress,
        onRestoreApplied,
        fileName,
        metronomeBpmTouched
    ) {
        bridge.onAsciiTabCallback = onAsciiTabGenerated
        bridge.onTabAnalysisCallback = onTabAnalysis
        bridge.onCompactTabsCallback = onCompactTabsGenerated
        bridge.onJsReadyCallback = {
            Handler(Looper.getMainLooper()).post {
                webEntry.jsReady = true
                jsReadyAtMs = System.currentTimeMillis()
                if (ENABLE_TAB_PERF_TRACE) {
                    Log.d(loadTag, "onJsReady file=$fileName")
                }
                isReady = true
            }
        }
        bridge.onScoreLoadedCallback = { totalMeasuresInternal ->
            Handler(Looper.getMainLooper()).post {
                if (totalMeasuresInternal <= 0) {
                    if (ENABLE_TAB_PERF_TRACE) {
                        Log.d(loadTag, "ignore non-final onScoreLoaded file=$fileName bars=$totalMeasuresInternal source=$loadedSourceForCurrentLesson")
                    }
                    return@post
                }
                isScoreLoaded = true
                tabViewModel.markScoreLoaded(totalMeasuresInternal)
                totalBars = totalMeasuresInternal
                webEntry.loadedTotalMeasures = totalMeasuresInternal
                webEntry.loadedFileName = fileName
                TabLoadMetricsTracker.markScoreLoaded(tabId, loadedSourceForCurrentLesson)
                val now = System.currentTimeMillis()
                val fromRequest = if (loadRequestAtMs > 0L) now - loadRequestAtMs else -1L
                val fromJsReady = if (jsReadyAtMs > 0L) now - jsReadyAtMs else -1L
                if (ENABLE_TAB_PERF_TRACE) {
                    Log.d(
                        loadTag,
                        "onScoreLoaded file=$fileName bars=$totalMeasuresInternal fromRequestMs=$fromRequest fromJsReadyMs=$fromJsReady source=$loadedSourceForCurrentLesson"
                    )
                    Log.d(restoreTag, "onScoreLoaded(totalMeasures=$totalMeasuresInternal)")
                }
                onTotalMeasuresLoaded(totalMeasuresInternal)
            }
        }
        bridge.onDetectedTempoCallback = { bpm ->
            Handler(Looper.getMainLooper()).post {
                if (!metronomeBpmTouched) {
                    metronomeBpm = bpm.coerceIn(40, 240)
                }
            }
        }
        bridge.onAlphaTabStatusCallback = { message ->
            val important =
                ENABLE_TAB_PERF_TRACE &&
                    (
                        message.startsWith("init") ||
                            message.startsWith("apiNotReady") ||
                            message.startsWith("initError") ||
                            message.startsWith("restore:") ||
                            message.startsWith("error")
                        )
            if (important) {
                Log.d("AlphaTabStatus", message)
            }
            if (message.contains("soundFontLoaded")) {
                webEntry.soundFontLoaded = true
            }
        }
        bridge.onTickPositionCallback = { tick, playing ->
            webEntry.lastKnownTick = tick
            onTickPosition(tick, playing)
        }
        bridge.onPlaybackProgressCallback = { tick, playing, barIndex ->
            webEntry.lastKnownTick = tick
            if (barIndex > 0) {
                webEntry.lastKnownBarIndex = barIndex
            }
            onTickPosition(tick, playing)
            onPlaybackProgress(tick, playing, barIndex, totalBars)
        }
        bridge.onRestoreAppliedCallback = { tick, currentBarIndex, requestedBarIndex ->
            webEntry.lastKnownTick = tick
            if (currentBarIndex > 0) {
                webEntry.lastKnownBarIndex = currentBarIndex
            }
            if (ENABLE_TAB_PERF_TRACE) {
                Log.d(
                    restoreTag,
                    "JS onRestoreApplied(tick=$tick currentBar=$currentBarIndex requestedBar=$requestedBarIndex)"
                )
            }
            onRestoreApplied(tick, currentBarIndex, requestedBarIndex)
        }
        onDispose {}
    }

    DisposableEffect(Unit) {
        onDispose {
            bridge.onAsciiTabCallback = {}
            bridge.onTabAnalysisCallback = {}
            bridge.onCompactTabsCallback = {}
            bridge.onJsReadyCallback = {}
            bridge.onScoreLoadedCallback = {}
            bridge.onDetectedTempoCallback = {}
            bridge.onAlphaTabStatusCallback = {}
            bridge.onTickPositionCallback = { _, _ -> }
            bridge.onPlaybackProgressCallback = { _, _, _ -> }
            bridge.onRestoreAppliedCallback = { _, _, _ -> }
            runCatching { webView.evaluateJavascript("window.stopAudio();", null) }
            runCatching { webView.stopLoading() }
            runCatching { webView.loadUrl("about:blank") }
            runCatching { (webView.parent as? ViewGroup)?.removeView(webView) }
            runCatching { webView.destroy() }
        }
    }

    LaunchedEffect(fileName) {
        isReady = webEntry.jsReady
        val canReuseRendered =
            webEntry.jsReady &&
                webEntry.loadedTotalMeasures > 0 &&
                webEntry.loadedFileName == fileName
        if (canReuseRendered) {
            loadedSourceForCurrentLesson = "reused-webview"
            isScoreLoaded = true
            tabViewModel.markScoreLoaded(webEntry.loadedTotalMeasures)
            isReady = true
            totalBars = webEntry.loadedTotalMeasures
            TabLoadMetricsTracker.markScoreLoaded(tabId, "reused-webview")
            onTotalMeasuresLoaded(webEntry.loadedTotalMeasures)
            if (ENABLE_TAB_PERF_TRACE) {
                Log.d(loadTag, "file change -> REUSE rendered score file=$fileName bars=${webEntry.loadedTotalMeasures}")
            }
        } else {
            loadedSourceForCurrentLesson = null
            isScoreLoaded = false
            tabViewModel.markScoreLoading()
            if (ENABLE_TAB_PERF_TRACE) {
                Log.d(
                    loadTag,
                    "file change -> COLD load file=$fileName entry={jsReady=${webEntry.jsReady}, bars=${webEntry.loadedTotalMeasures}, loadedFile=${webEntry.loadedFileName}}"
                )
            }
        }
        metronomeBpmTouched = false
        loadRequestAtMs = 0L
        jsReadyAtMs = 0L
        if (ENABLE_TAB_PERF_TRACE) {
            Log.d(loadTag, "file change -> reset timing markers file=$fileName")
        }
    }

    var previousFrameNanos by remember(fileName) { mutableStateOf(0L) }
    LaunchedEffect(fileName) {
        if (!ENABLE_TAB_PERF_TRACE) return@LaunchedEffect
        val startedAtMs = System.currentTimeMillis()
        var frameIndex = 0
        previousFrameNanos = 0L
        while (isActive && System.currentTimeMillis() - startedAtMs < 7000L) {
            androidx.compose.runtime.withFrameNanos { now ->
                if (previousFrameNanos != 0L) {
                    val frameMs = (now - previousFrameNanos) / 1_000_000.0
                    if (frameMs > 24.0) {
                        Log.w(frameTag, "file=$fileName frameIndex=$frameIndex frameMs=${"%.1f".format(frameMs)}")
                    }
                }
                previousFrameNanos = now
                frameIndex++
            }
        }
        Log.d(frameTag, "file=$fileName monitorWindowMs=7000 completed")
    }

    LaunchedEffect(restorePending, restoreBarIndex, fileName) {
        val targetBar = restoreBarIndex ?: -1
        val lastKnownBarIndex = webEntry.lastKnownBarIndex
        val canResolveFromReuse =
            restorePending &&
                loadedSourceForCurrentLesson == "reused-webview" &&
                targetBar > 0 &&
                lastKnownBarIndex > 0
        if (canResolveFromReuse) {
            val diff = kotlin.math.abs(lastKnownBarIndex - targetBar)
            if (diff <= 1) {
                if (ENABLE_TAB_PERF_TRACE) {
                    Log.d(
                        restoreTag,
                        "reuse fast-path restore resolved locally targetBar=$targetBar currentBar=$lastKnownBarIndex"
                    )
                }
                onRestoreApplied(webEntry.lastKnownTick, lastKnownBarIndex, targetBar)
            }
        }
    }

    LaunchedEffect(isScoreLoaded) {
        showLoadingOverlay = !isScoreLoaded
    }

    // Load SoundFont + score only once per lesson to avoid late resets of cursor after restore.
    LaunchedEffect(fileName, tabBytesReady, soundFontReady, isReady, loadedSourceForCurrentLesson) {
        if (isReady) {
            val modeStr = when (tabDisplayMode) {
                TabDisplayMode.NOTES_ONLY -> "Score"
                TabDisplayMode.TAB_ONLY -> "Tab"
                TabDisplayMode.TAB_AND_NOTES -> "ScoreTab"
            }
            webView.evaluateJavascript("window.initSettings($isDark, $isPracticeMode, $currentSpeed, $currentScale, '$modeStr', '$jsLanguageTag');", null)
            if (loadedSourceForCurrentLesson == null) {
                isScoreLoaded = false
                tabViewModel.markScoreLoading()
                // Read bytes directly from ViewModel volatile field — not via Compose state
                if (soundFontReady && !webEntry.soundFontLoaded) {
                    val soundFont = tabViewModel.soundFontRef
                    if (soundFont != null) {
                        if (ENABLE_TAB_PERF_TRACE) Log.d(restoreTag, "loading soundfont before score")
                        webView.evaluateJavascript("window.loadSoundFontFromBase64('$soundFont');", null)
                    }
                } else if (webEntry.soundFontLoaded) {
                    if (ENABLE_TAB_PERF_TRACE) Log.d(loadTag, "skip soundfont reload (already loaded in reused WebView)")
                }
                val isAbsoluteLocalFile = fileName.startsWith("/")
                val isBundledAssetRelative = !fileName.contains("://") && !isAbsoluteLocalFile
                if (isBundledAssetRelative) {
                    val assetUrl = "https://appassets.androidplatform.net/assets/$fileName"
                    loadedSourceForCurrentLesson = "asset-url"
                    loadRequestAtMs = System.currentTimeMillis()
                    TabLoadMetricsTracker.markLoadRequested(tabId, "asset-url")
                    if (ENABLE_TAB_PERF_TRACE) {
                        Log.d(restoreTag, "loading score from asset url")
                        Log.d(loadTag, "request load assetUrl=$assetUrl")
                    }
                    webView.evaluateJavascript("window.loadTab('$assetUrl');", null)
                } else if (tabBytesReady) {
                    val base64 = tabViewModel.tabBytesRef
                    if (base64 != null) {
                        loadedSourceForCurrentLesson = "base64"
                        loadRequestAtMs = System.currentTimeMillis()
                        TabLoadMetricsTracker.markLoadRequested(tabId, "base64")
                        if (ENABLE_TAB_PERF_TRACE) {
                            Log.d(restoreTag, "loading score from base64")
                            Log.d(loadTag, "request load base64 file=$fileName len=${base64.length}")
                        }
                        webView.evaluateJavascript("window.loadTabFromBase64('$base64');", null)
                    }
                } else if (isAbsoluteLocalFile) {
                    // Fallback only. In WebView appassets context local file:// may be blocked on some devices.
                    loadedSourceForCurrentLesson = "file-fallback"
                    loadRequestAtMs = System.currentTimeMillis()
                    TabLoadMetricsTracker.markLoadRequested(tabId, "file-fallback")
                    if (ENABLE_TAB_PERF_TRACE) {
                        Log.d(restoreTag, "loading score from file fallback")
                        Log.d(loadTag, "request load file fallback path=$fileName")
                    }
                    webView.evaluateJavascript("window.loadTab('$fileName');", null)
                } else {
                    if (ENABLE_TAB_PERF_TRACE) {
                        Log.d(restoreTag, "waiting for base64 bytes, skip file fallback for bundled tab path: $fileName")
                        Log.d(loadTag, "waiting for tab bytes file=$fileName")
                    }
                }
            }
        }
    }

    // React to settings changes explicitly in one combined effect to avoid frame spam
    LaunchedEffect(isPracticeMode, currentSpeed, currentScale, tabDisplayMode, isReady, restorePending, languageTag) {
        if (isReady && !restorePending) {
            val modeStr = when (tabDisplayMode) {
                TabDisplayMode.NOTES_ONLY -> "Score"
                TabDisplayMode.TAB_ONLY -> "Tab"
                TabDisplayMode.TAB_AND_NOTES -> "ScoreTab"
            }
            webView.evaluateJavascript("window.initSettings($isDark, $isPracticeMode, $currentSpeed, $currentScale, '$modeStr', '$jsLanguageTag');", null)
        }
    }

    LaunchedEffect(silentMode, isReady) {
        if (isReady) {
            webView.evaluateJavascript("window.setSilentMode($silentMode);", null)
        }
    }

    LaunchedEffect(restorePending, isReady) {
        if (isReady) {
            webView.evaluateJavascript("window.setRestoreLock($restorePending);", null)
        }
    }

    LaunchedEffect(metronomeEnabled, isReady) {
        if (isReady) {
            webView.evaluateJavascript("window.setMetronomeEnabled($metronomeEnabled);", null)
        }
    }

    LaunchedEffect(metronomeBpm, isReady) {
        if (isReady) {
            webView.evaluateJavascript("window.setMetronomeBpm($metronomeBpm);", null)
        }
    }

    LaunchedEffect(restoreTickPosition, restoreBarIndex, wasPlaying, restorePending, isReady, isScoreLoaded) {
        if (isReady && isScoreLoaded && restorePending) {
            val tick = restoreTickPosition ?: 0L
            val barIndex = restoreBarIndex ?: -1
            val playFlag = if (wasPlaying) "true" else "false"
            if (ENABLE_TAB_PERF_TRACE) {
                Log.d(
                    restoreTag,
                    "restore effect start: tick=$tick barIndex=$barIndex play=$playFlag isScoreLoaded=$isScoreLoaded"
                )
                Log.d(restoreTag, "restore request sent")
            }
            webView.evaluateJavascript("window.setRestorePlayback($tick, $playFlag, $barIndex);", null)
        }
    }

    LaunchedEffect(isLandscape, isReady) {
        if (isReady) {
            val flag = if (isLandscape) "true" else "false"
            webView.evaluateJavascript("window.setOrientation($flag);", null)
        }
    }

    LaunchedEffect(isLoopEnabled, loopStartMeasure, loopEndMeasure, isReady) {
        if (isReady) {
            webView.evaluateJavascript("window.setLoopRange($loopStartMeasure, $loopEndMeasure, $isLoopEnabled);", null)
        }
    }

    LaunchedEffect(isDark) {
        webView.setBackgroundColor(if (isDark) android.graphics.Color.parseColor("#1c1b1f") else android.graphics.Color.WHITE)
        if (isReady) {
            webView.evaluateJavascript("window.setTheme($isDark);", null)
        }
    }

    val isReusedSession = loadedSourceForCurrentLesson == "reused-webview"
    val controlsVisible = isScoreLoaded && (!restorePending || isReusedSession)
    TabViewerViewport(
        modifier = modifier,
        webView = webView,
        isDark = isDark,
        showLoadingOverlay = showLoadingOverlay,
        restorePending = restorePending,
        isReusedSession = isReusedSession,
        isPlaying = isPlaying,
        controlsVisible = controlsVisible,
        onPlayPause = {
            webView.evaluateJavascript("window.playPause();", null)
            onPlayStateChange(!isPlaying)
        },
        onOpenDisplaySheet = { showDisplaySheet = true },
        onOpenLearningSheet = { showLearningSheet = true },
        onWebYChanged = { currentY ->
            val previous = lastWebY
            if (ENABLE_TAB_PERF_TRACE && previous != null && kotlin.math.abs(previous - currentY) >= 8) {
                Log.d(layoutTag, "webViewY shift file=$fileName from=$previous to=$currentY")
            }
            lastWebY = currentY
        },
        onControlsYChanged = { currentY ->
            val previous = lastControlsY
            if (ENABLE_TAB_PERF_TRACE && previous != null && kotlin.math.abs(previous - currentY) >= 8) {
                Log.d(layoutTag, "controlsY shift file=$fileName from=$previous to=$currentY")
            }
            lastControlsY = currentY
        },
        totalBars = totalBars.coerceAtLeast(1)
    )

    TabViewerSheets(
        showDisplaySheet = showDisplaySheet,
        onDismissDisplaySheet = { showDisplaySheet = false },
        displaySheetState = displaySheetState,
        currentSpeed = currentSpeed,
        currentScale = currentScale,
        tabDisplayMode = tabDisplayMode,
        onSpeedChange = onSpeedChange,
        onScaleChange = onScaleChange,
        onTabDisplayModeChange = onTabDisplayModeChange,
        silentMode = silentMode,
        onSilentModeChange = onSilentModeChange,
        showLearningSheet = showLearningSheet,
        onDismissLearningSheet = { showLearningSheet = false },
        learningSheetState = learningSheetState,
        onOpenAiAssistant = onOpenAiAssistant,
        onOpenNotes = onOpenNotes,
        onOpenLoop = onOpenLoop,
        metronomeEnabled = metronomeEnabled,
        metronomeBpm = metronomeBpm,
        onMetronomeEnabledChange = { metronomeEnabled = it },
        onMetronomeBpmChange = {
            metronomeBpmTouched = true
            metronomeBpm = it
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabViewerViewport(
    modifier: Modifier,
    webView: WebView,
    isDark: Boolean,
    showLoadingOverlay: Boolean,
    restorePending: Boolean,
    isReusedSession: Boolean,
    isPlaying: Boolean,
    controlsVisible: Boolean,
    onPlayPause: () -> Unit,
    onOpenDisplaySheet: () -> Unit,
    onOpenLearningSheet: () -> Unit,
    onWebYChanged: (Int) -> Unit,
    onControlsYChanged: (Int) -> Unit,
    totalBars: Int
) {
    Box(modifier = modifier) {
        val shouldShowOverlay = showLoadingOverlay || (restorePending && !isReusedSession)

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = {
                        (webView.parent as? ViewGroup)?.removeView(webView)
                        webView.animate().cancel()
                        webView.alpha = if (shouldShowOverlay) 0f else 1f
                        webView
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { coords ->
                            onWebYChanged(coords.boundsInRoot().top.toInt())
                        },
                    update = { view ->
                        view.setBackgroundColor(if (isDark) android.graphics.Color.parseColor("#1c1b1f") else android.graphics.Color.WHITE)
                        val targetVisible = !shouldShowOverlay
                        if (targetVisible) {
                            if (view.alpha != 1f) {
                                view.animate().cancel()
                                view.animate().alpha(1f).setDuration(140L).start()
                            }
                        } else {
                            view.animate().cancel()
                            view.alpha = 0f
                        }
                    }
                )

            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (controlsVisible) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coords ->
                                onControlsYChanged(coords.boundsInRoot().top.toInt())
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        RoundControlButton(
                            onClick = onPlayPause,
                            icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play),
                            backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                            iconTint = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RoundControlButton(
                                onClick = onOpenDisplaySheet,
                                icon = Icons.Filled.Visibility,
                                contentDescription = stringResource(R.string.display_controls),
                                backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                                iconTint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            RoundControlButton(
                                onClick = onOpenLearningSheet,
                                icon = Icons.Filled.School,
                                contentDescription = stringResource(R.string.learning_controls),
                                backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                                iconTint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}
