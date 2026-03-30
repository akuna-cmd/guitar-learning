package com.example.thetest1.presentation.tab_viewer

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.thetest1.R
import com.example.thetest1.BuildConfig
import com.example.thetest1.di.ViewModelFactory
import com.example.thetest1.presentation.ai_assistant.AiAssistantScreen
import com.example.thetest1.presentation.main.MainViewModel
import com.example.thetest1.presentation.notes.NotesScreen
import com.example.thetest1.presentation.theory.TheoryScreen
import com.example.thetest1.presentation.main.ThemeViewModel
import com.example.thetest1.presentation.main.TabDisplayMode
import com.example.thetest1.presentation.main.ThemeUiState
import com.example.thetest1.presentation.main.FretboardDisplayMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

private const val ENABLE_TAB_PERF_TRACE = false

class TabJsBridge {
    var onAsciiTabCallback: (String) -> Unit = {}
    var onTabAnalysisCallback: (String) -> Unit = {}
    var onCompactTabsCallback: (String) -> Unit = {}
    var onJsReadyHostCallback: () -> Unit = {}
    var onJsReadyCallback: () -> Unit = {}
    var onScoreLoadedHostCallback: (Int) -> Unit = {}
    var onScoreLoadedCallback: (Int) -> Unit = {}
    var onDetectedTempoCallback: (Int) -> Unit = {}
    var onAlphaTabStatusHostCallback: (String) -> Unit = {}
    var onAlphaTabStatusCallback: (String) -> Unit = {}
    var onTickPositionCallback: (Long, Boolean) -> Unit = { _, _ -> }
    var onPlaybackProgressCallback: (Long, Boolean, Int) -> Unit = { _, _, _ -> }
    var onRestoreAppliedCallback: (Long, Int, Int) -> Unit = { _, _, _ -> }

    @JavascriptInterface fun postAsciiTab(ascii: String) = onAsciiTabCallback(ascii)
    @JavascriptInterface fun postTabAnalysis(json: String) = onTabAnalysisCallback(json)
    @JavascriptInterface fun postCompactTabs(tabs: String) = onCompactTabsCallback(tabs)
    @JavascriptInterface
    fun onJsReady() {
        onJsReadyHostCallback.invoke()
        onJsReadyCallback.invoke()
    }
    @JavascriptInterface
    fun onScoreLoaded(totalMeasures: Int) {
        onScoreLoadedHostCallback(totalMeasures)
        onScoreLoadedCallback(totalMeasures)
    }
    @JavascriptInterface fun onDetectedTempo(bpm: Int) = onDetectedTempoCallback(bpm)
    @JavascriptInterface
    fun onAlphaTabStatus(message: String) {
        onAlphaTabStatusHostCallback(message)
        onAlphaTabStatusCallback(message)
    }
    @JavascriptInterface fun onTickPosition(tick: Long, isPlaying: Boolean) = onTickPositionCallback(tick, isPlaying)
    @JavascriptInterface fun onPlaybackProgress(tick: Long, isPlaying: Boolean, barIndex: Int) =
        onPlaybackProgressCallback(tick, isPlaying, barIndex)
    @JavascriptInterface fun onRestoreApplied(tick: Long, currentBarIndex: Int, requestedBarIndex: Int) =
        onRestoreAppliedCallback(tick, currentBarIndex, requestedBarIndex)
}

data class TabWebViewEntry(
    val webView: WebView,
    val bridge: TabJsBridge,
    var jsReady: Boolean = false,
    var loadedTotalMeasures: Int = 0,
    var soundFontLoaded: Boolean = false,
    var loadedFileName: String? = null,
    var lastKnownTick: Long = 0L,
    var lastKnownBarIndex: Int = 0
)

private fun createTabWebViewEntry(context: Context): TabWebViewEntry {
    val assetLoader = WebViewAssetLoader.Builder()
        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context.applicationContext))
        .build()

    val webView = WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        setBackgroundColor(android.graphics.Color.parseColor("#1c1b1f"))

        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
            mediaPlaybackRequiresUserGesture = false
        }

        webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                val message = consoleMessage?.message().orEmpty()
                if (
                    BuildConfig.DEBUG &&
                    ENABLE_TAB_PERF_TRACE &&
                    (
                        message.startsWith("AlphaTabStatus:init") ||
                            message.startsWith("AlphaTabStatus:initError") ||
                            message.startsWith("AlphaTabStatus:apiNotReady") ||
                            message.startsWith("AlphaTabStatus:error") ||
                            message.startsWith("AlphaTabStatus:restore:")
                        )
                ) {
                    Log.d("WebViewConsole", message)
                }
                return true
            }
        }

        webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: android.webkit.WebResourceRequest?
            ): android.webkit.WebResourceResponse? {
                val url = request?.url ?: return null
                return assetLoader.shouldInterceptRequest(url)
            }

            override fun onPageFinished(view: WebView?, url: String?) {}
        }
    }

    val bridge = TabJsBridge()
    lateinit var entry: TabWebViewEntry
    bridge.onJsReadyHostCallback = {
        entry.jsReady = true
    }
    bridge.onScoreLoadedHostCallback = { totalMeasures ->
        if (totalMeasures > 0) {
            entry.loadedTotalMeasures = totalMeasures
        }
    }
    bridge.onAlphaTabStatusHostCallback = { message ->
        if (message.contains("soundFontLoaded")) {
            entry.soundFontLoaded = true
        }
    }
    webView.addJavascriptInterface(bridge, "Android")
    webView.loadUrl("https://appassets.androidplatform.net/assets/tab_viewer.html")
    entry = TabWebViewEntry(webView = webView, bridge = bridge)
    return entry
}

private data class TabViewerModel(
    val fileName: String,
    val tabBytesReady: Boolean,
    val soundFontReady: Boolean,
    val tabTitle: String,
    val isPracticeMode: Boolean,
    val currentSpeed: Float,
    val tabDisplayMode: TabDisplayMode,
    val lastTickPosition: Long?,
    val lastBarIndex: Int?,
    val restoreTickPosition: Long?,
    val restoreBarIndex: Int?,
    val totalBars: Int?,
    val restorePending: Boolean,
    val wasPlaying: Boolean,
    val currentScale: Float,
    val silentMode: Boolean,
    val themeUiState: ThemeUiState,
    val isPlaying: Boolean,
    val loopStartMeasure: Int,
    val loopEndMeasure: Int,
    val isLoopEnabled: Boolean
)

private data class TabViewerHandlers(
    val onSpeedChange: (Float) -> Unit,
    val onTabDisplayModeChange: (TabDisplayMode) -> Unit,
    val onRestoreApplied: (Long, Int, Int) -> Unit,
    val onTickPosition: (Long, Boolean) -> Unit,
    val onPlaybackProgress: (Long, Boolean, Int, Int) -> Unit,
    val onScaleChange: (Float) -> Unit,
    val onSilentModeChange: (Boolean) -> Unit,
    val onOpenAiAssistant: () -> Unit,
    val onOpenNotes: () -> Unit,
    val onOpenLoop: () -> Unit,
    val onPlayStateChange: (Boolean) -> Unit,
    val onAsciiTabGenerated: (String) -> Unit,
    val onTabAnalysis: (String) -> Unit,
    val onCompactTabsGenerated: (String) -> Unit,
    val onTotalMeasuresLoaded: (Int) -> Unit,
    val onLoopRangeChangedFromGesture: (Int, Int) -> Unit,
    val isLoopGestureSelectionArmed: Boolean,
    val onLoopGestureSelectionDone: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabViewerScreen(
    lessonId: String,
    viewModelFactory: ViewModelFactory,
    mainViewModel: MainViewModel,
    themeViewModel: ThemeViewModel,
    onBack: () -> Unit
) {
    val viewModel: TabViewerViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
    }

    lesson?.let {
        LaunchedEffect(it) {
            mainViewModel.setActiveTab(it.id, it.title)
        }
    }

    var showAiSheet by remember { mutableStateOf(false) }
    var showNotesSheet by remember { mutableStateOf(false) }
    var showLoopSheet by remember { mutableStateOf(false) }
    var totalMeasures by remember { mutableStateOf(1) }
    var loopStartMeasure by remember { mutableStateOf(1) }
    var loopEndMeasure by remember { mutableStateOf(1) }
    var isLoopEnabled by remember { mutableStateOf(false) }
    var isLoopGestureSelectionArmed by remember { mutableStateOf(false) }
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
                        val modes = listOf(false to "Звичайна гра", true to "Режим розбору")
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
                            },
                            onLoopRangeChangedFromGesture = { start, end ->
                                loopStartMeasure = start
                                loopEndMeasure = end
                                isLoopEnabled = true
                            },
                            isLoopGestureSelectionArmed = isLoopGestureSelectionArmed,
                            onLoopGestureSelectionDone = { isLoopGestureSelectionArmed = false }
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
                                viewModelFactory = viewModelFactory,
                                asciiTab = uiState.asciiTab,
                                compactTabs = uiState.compactTabs,
                                totalMeasures = totalMeasures,
                                initialMeasureRange = if (isLoopEnabled) loopStartMeasure..loopEndMeasure else null
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
                                audioNotes = uiState.audioNotes,
                                textNotes = uiState.textNotes,
                                isRecording = uiState.isRecording,
                                playerState = uiState.playerState,
                                onAddAudioNote = { uri -> viewModel.addAudioNoteFromFile(lesson.id, uri) },
                                onRecordAudio = { viewModel.onRecordAudio(lesson.id) },
                                onDeleteAudioNote = { id -> viewModel.deleteAudioNote(id) },
                                onPlayAudio = { note -> viewModel.onPlayAudio(note) },
                                onSeekAudio = { id, prog -> viewModel.onSeekAudio(id, prog) },
                                onAddTextNote = { content -> viewModel.addTextNote(lesson.id, content) },
                                onUpdateTextNote = { note -> viewModel.updateTextNote(note) },
                                onDeleteTextNote = { note -> viewModel.deleteTextNote(note) }
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
                                onToggleLoop = { isLoopEnabled = it },
                                onPickRangeOnScore = {
                                    isLoopGestureSelectionArmed = true
                                    showLoopSheet = false
                                }
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
    val onLoopRangeChangedFromGesture = handlers.onLoopRangeChangedFromGesture
    val isLoopGestureSelectionArmed = handlers.isLoopGestureSelectionArmed
    val onLoopGestureSelectionDone = handlers.onLoopGestureSelectionDone

    val restoreTag = "TabRestoreFlow"
    val loadTag = "TabLoadPerf"
    val layoutTag = "TabLayoutTrace"
    val frameTag = "TabFramePerf"
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeUiState.themeMode) {
        com.example.thetest1.presentation.main.ThemeMode.DARK -> true
        com.example.thetest1.presentation.main.ThemeMode.LIGHT -> false
        com.example.thetest1.presentation.main.ThemeMode.SYSTEM -> isSystemDark
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
            webView.evaluateJavascript("window.initSettings($isDark, $isPracticeMode, $currentSpeed, $currentScale, '$modeStr');", null)
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
    LaunchedEffect(isPracticeMode, currentSpeed, currentScale, tabDisplayMode, isReady, restorePending) {
        if (isReady && !restorePending) {
            val modeStr = when (tabDisplayMode) {
                TabDisplayMode.NOTES_ONLY -> "Score"
                TabDisplayMode.TAB_ONLY -> "Tab"
                TabDisplayMode.TAB_AND_NOTES -> "ScoreTab"
            }
            webView.evaluateJavascript("window.initSettings($isDark, $isPracticeMode, $currentSpeed, $currentScale, '$modeStr');", null)
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
        totalBars = totalBars.coerceAtLeast(1),
        isLoopGestureSelectionArmed = isLoopGestureSelectionArmed,
        onLoopGestureSelection = { start, end ->
            onLoopRangeChangedFromGesture(start, end)
            onLoopGestureSelectionDone()
        }
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
    totalBars: Int,
    isLoopGestureSelectionArmed: Boolean,
    onLoopGestureSelection: (Int, Int) -> Unit
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

                if (isLoopGestureSelectionArmed) {
                    var firstPointMeasure by remember { mutableStateOf<Int?>(null) }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            .pointerInput(totalBars) {
                                detectTapGestures { offset ->
                                    if (size.width <= 0f) return@detectTapGestures
                                    val tappedMeasure = (((offset.x / size.width) * totalBars).toInt() + 1)
                                        .coerceIn(1, totalBars)
                                    val first = firstPointMeasure
                                    if (first == null) {
                                        firstPointMeasure = tappedMeasure
                                    } else {
                                        onLoopGestureSelection(
                                            minOf(first, tappedMeasure),
                                            maxOf(first, tappedMeasure)
                                        )
                                        firstPointMeasure = null
                                    }
                                }
                            },
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Text(
                            text = if (firstPointMeasure == null) {
                                "Точка 1: тапни перший такт"
                            } else {
                                "Точка 2: тапни останній такт (від ${firstPointMeasure})"
                            },
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier
                                .padding(top = 10.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabViewerSheets(
    showDisplaySheet: Boolean,
    onDismissDisplaySheet: () -> Unit,
    displaySheetState: androidx.compose.material3.SheetState,
    currentSpeed: Float,
    currentScale: Float,
    tabDisplayMode: TabDisplayMode,
    onSpeedChange: (Float) -> Unit,
    onScaleChange: (Float) -> Unit,
    onTabDisplayModeChange: (TabDisplayMode) -> Unit,
    silentMode: Boolean,
    onSilentModeChange: (Boolean) -> Unit,
    showLearningSheet: Boolean,
    onDismissLearningSheet: () -> Unit,
    learningSheetState: androidx.compose.material3.SheetState,
    onOpenAiAssistant: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenLoop: () -> Unit,
    metronomeEnabled: Boolean,
    metronomeBpm: Int,
    onMetronomeEnabledChange: (Boolean) -> Unit,
    onMetronomeBpmChange: (Int) -> Unit
) {
    if (showDisplaySheet) {
        ModalBottomSheet(
            onDismissRequest = onDismissDisplaySheet,
            sheetState = displaySheetState
        ) {
            DisplayControlsSheet(
                currentSpeed = currentSpeed,
                currentScale = currentScale,
                tabDisplayMode = tabDisplayMode,
                onSpeedChange = onSpeedChange,
                onScaleChange = onScaleChange,
                onTabDisplayModeChange = onTabDisplayModeChange,
                silentMode = silentMode,
                onSilentModeChange = onSilentModeChange
            )
        }
    }

    if (showLearningSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismissLearningSheet,
            sheetState = learningSheetState
        ) {
            LearningControlsSheet(
                onOpenAiAssistant = {
                    onDismissLearningSheet()
                    onOpenAiAssistant()
                },
                onOpenNotes = {
                    onDismissLearningSheet()
                    onOpenNotes()
                },
                onOpenLoop = {
                    onDismissLearningSheet()
                    onOpenLoop()
                },
                metronomeEnabled = metronomeEnabled,
                metronomeBpm = metronomeBpm,
                onMetronomeEnabledChange = onMetronomeEnabledChange,
                onMetronomeBpmChange = onMetronomeBpmChange
            )
        }
    }
}

@Composable
private fun RoundControlButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    backgroundColor: Color,
    iconTint: Color
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
    }
}

private fun speedString(speed: Float): String {
    return String.format("%.1f", speed).replace(',', '.')
}

private fun stepSpeed(value: Float, delta: Float): Float {
    val stepped = ((value + delta) * 10f).toInt() / 10f
    return stepped.coerceIn(0.1f, 2.5f)
}

private fun scaleString(scale: Float): String {
    return String.format("%.1f", scale).replace(',', '.')
}

private fun stepBpm(value: Int, delta: Int): Int {
    return (value + delta).coerceIn(40, 240)
}

@Composable
private fun DisplayControlsSheet(
    currentSpeed: Float,
    currentScale: Float,
    tabDisplayMode: TabDisplayMode,
    onSpeedChange: (Float) -> Unit,
    onScaleChange: (Float) -> Unit,
    onTabDisplayModeChange: (TabDisplayMode) -> Unit,
    silentMode: Boolean,
    onSilentModeChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = stringResource(R.string.display_controls), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Speed, contentDescription = null)
            Text(text = stringResource(R.string.speed_value_format, speedString(currentSpeed)), modifier = Modifier.weight(1f))
            HoldableIconButton(onClick = { onSpeedChange(stepSpeed(currentSpeed, -0.1f)) }, contentDescription = stringResource(R.string.speed_decrease), icon = Icons.Default.Remove)
            HoldableIconButton(onClick = { onSpeedChange(stepSpeed(currentSpeed, 0.1f)) }, contentDescription = stringResource(R.string.speed_increase), icon = Icons.Default.Add)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.ZoomIn, contentDescription = null)
            Text(text = stringResource(R.string.scale_value_format, scaleString(currentScale)), modifier = Modifier.weight(1f))
            HoldableIconButton(onClick = { onScaleChange(stepScale(currentScale, -0.1f)) }, contentDescription = stringResource(R.string.scale_decrease), icon = Icons.Default.Remove)
            HoldableIconButton(onClick = { onScaleChange(stepScale(currentScale, 0.1f)) }, contentDescription = stringResource(R.string.scale_increase), icon = Icons.Default.Add)
        }
        Text(text = stringResource(R.string.display_mode), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(
                onClick = { onTabDisplayModeChange(TabDisplayMode.TAB_AND_NOTES) },
                label = { Text(stringResource(R.string.tab_display_mode_tab_and_notes)) },
                leadingIcon = { Icon(Icons.Default.LibraryMusic, contentDescription = null) }
            )
            AssistChip(
                onClick = { onTabDisplayModeChange(TabDisplayMode.NOTES_ONLY) },
                label = { Text(stringResource(R.string.tab_display_mode_notes_only)) },
                leadingIcon = { Icon(Icons.Default.MusicNote, contentDescription = null) }
            )
            AssistChip(
                onClick = { onTabDisplayModeChange(TabDisplayMode.TAB_ONLY) },
                label = { Text(stringResource(R.string.tab_display_mode_tab_only)) },
                leadingIcon = { Icon(Icons.Default.QueueMusic, contentDescription = null) }
            )
        }
        ListItem(
            headlineContent = { Text(stringResource(R.string.silent_mode)) },
            supportingContent = { Text(stringResource(R.string.silent_mode_desc)) },
            trailingContent = {
                Switch(
                    checked = silentMode,
                    onCheckedChange = onSilentModeChange
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun LearningControlsSheet(
    onOpenAiAssistant: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenLoop: () -> Unit,
    metronomeEnabled: Boolean,
    metronomeBpm: Int,
    onMetronomeEnabledChange: (Boolean) -> Unit,
    onMetronomeBpmChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = stringResource(R.string.learning_controls), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        ListItem(
            headlineContent = { Text(stringResource(R.string.notes)) },
            leadingContent = { Icon(Icons.Filled.NoteAdd, contentDescription = null) },
            modifier = Modifier.clickable { onOpenNotes() }
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.loop_section)) },
            leadingContent = { Icon(Icons.Filled.Repeat, contentDescription = null) },
            modifier = Modifier.clickable { onOpenLoop() }
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.metronome)) },
            supportingContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = stringResource(R.string.metronome_bpm, metronomeBpm), modifier = Modifier.weight(1f))
                    HoldableIconButton(
                        onClick = { onMetronomeBpmChange(stepBpm(metronomeBpm, -5)) },
                        contentDescription = stringResource(R.string.metronome_decrease),
                        icon = Icons.Default.Remove
                    )
                    HoldableIconButton(
                        onClick = { onMetronomeBpmChange(stepBpm(metronomeBpm, 5)) },
                        contentDescription = stringResource(R.string.metronome_increase),
                        icon = Icons.Default.Add
                    )
                }
            },
            leadingContent = { Icon(Icons.Filled.MusicNote, contentDescription = null) },
            trailingContent = {
                Switch(
                    checked = metronomeEnabled,
                    onCheckedChange = onMetronomeEnabledChange
                )
            }
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.ai_assistant)) },
            leadingContent = { Icon(Icons.Filled.AutoAwesome, contentDescription = null) },
            modifier = Modifier.clickable { onOpenAiAssistant() }
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}

private fun stepScale(value: Float, delta: Float): Float {
    val stepped = ((value + delta) * 10f).toInt() / 10f
    return stepped.coerceIn(0.5f, 2.0f)
}

@Composable
private fun TabDisplayModeMenu(
    currentMode: TabDisplayMode,
    onModeChange: (TabDisplayMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.QueueMusic,
                contentDescription = stringResource(R.string.tab_display_mode_toggle),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = when (currentMode) {
                    TabDisplayMode.TAB_AND_NOTES -> stringResource(R.string.tab_display_mode_tab_and_notes)
                    TabDisplayMode.NOTES_ONLY -> stringResource(R.string.tab_display_mode_notes_only)
                    TabDisplayMode.TAB_ONLY -> stringResource(R.string.tab_display_mode_tab_only)
                },
                style = MaterialTheme.typography.labelLarge
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.tab_display_mode_tab_and_notes)) },
                onClick = {
                    expanded = false
                    onModeChange(TabDisplayMode.TAB_AND_NOTES)
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.tab_display_mode_notes_only)) },
                onClick = {
                    expanded = false
                    onModeChange(TabDisplayMode.NOTES_ONLY)
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.tab_display_mode_tab_only)) },
                onClick = {
                    expanded = false
                    onModeChange(TabDisplayMode.TAB_ONLY)
                }
            )
        }
    }
}

@Composable
private fun SpeedScaleMenu(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    valueText: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    decrementContentDescription: String,
    incrementContentDescription: String
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable { expanded = true }
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(text = valueText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HoldableIconButton(
                    onClick = onDecrement,
                    contentDescription = decrementContentDescription,
                    icon = Icons.Default.Remove
                )
                Text(text = valueText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                HoldableIconButton(
                    onClick = onIncrement,
                    contentDescription = incrementContentDescription,
                    icon = Icons.Default.Add
                )
            }
        }
    }
}

@Composable
private fun HoldableIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(32.dp)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown()
                        job?.cancel()
                        job = scope.launch {
                            delay(250)
                            while (down.pressed) {
                                onClick()
                                delay(80)
                            }
                        }
                        waitForUpOrCancellation()
                        job?.cancel()
                    }
                }
            }
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun LoopConfigurator(
    totalMeasures: Int,
    startMeasure: Int,
    endMeasure: Int,
    isLoopEnabled: Boolean,
    onStartChange: (Int) -> Unit,
    onEndChange: (Int) -> Unit,
    onToggleLoop: (Boolean) -> Unit,
    onPickRangeOnScore: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        androidx.compose.material3.Text("Зациклити відрізок", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onToggleLoop(!isLoopEnabled) }.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.Text("Увімкнути зациклення", style = MaterialTheme.typography.bodyLarge)
            androidx.compose.material3.Switch(
                checked = isLoopEnabled,
                onCheckedChange = { onToggleLoop(it) }
            )
        }
        Spacer(Modifier.height(16.dp))
        AssistChip(
            onClick = onPickRangeOnScore,
            label = { Text("Виділити пальцем на табах") },
            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
        )
        Spacer(Modifier.height(12.dp))
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.compose.material3.Text("Початковий такт: $startMeasure", fontWeight = FontWeight.Bold)
            androidx.compose.material3.Slider(
                value = startMeasure.toFloat(),
                onValueChange = { onStartChange(it.toInt().coerceAtMost(endMeasure)) },
                valueRange = 1f..totalMeasures.toFloat().coerceAtLeast(1f),
                steps = if (totalMeasures > 2) totalMeasures - 2 else 0
            )
        }
        Spacer(Modifier.height(8.dp))
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.compose.material3.Text("Кінцевий такт: $endMeasure", fontWeight = FontWeight.Bold)
            androidx.compose.material3.Slider(
                value = endMeasure.toFloat(),
                onValueChange = { onEndChange(it.toInt().coerceAtLeast(startMeasure)) },
                valueRange = 1f..totalMeasures.toFloat().coerceAtLeast(1f),
                steps = if (totalMeasures > 2) totalMeasures - 2 else 0
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}
