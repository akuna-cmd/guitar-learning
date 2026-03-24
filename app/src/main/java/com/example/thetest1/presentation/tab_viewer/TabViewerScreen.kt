package com.example.thetest1.presentation.tab_viewer

import android.annotation.SuppressLint
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
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.thetest1.R
import com.example.thetest1.di.ViewModelFactory
import com.example.thetest1.presentation.ai_assistant.AiAssistantScreen
import com.example.thetest1.presentation.main.MainViewModel
import com.example.thetest1.presentation.notes.NotesScreen
import com.example.thetest1.presentation.theory.TheoryScreen
import com.example.thetest1.presentation.main.ThemeViewModel
import com.example.thetest1.presentation.main.TabDisplayMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

private class TabJsBridge {
    var onAsciiTabCallback: (String) -> Unit = {}
    var onTabAnalysisCallback: (String) -> Unit = {}
    var onCompactTabsCallback: (String) -> Unit = {}
    var onJsReadyCallback: () -> Unit = {}
    var onScoreLoadedCallback: (Int) -> Unit = {}
    var onDetectedTempoCallback: (Int) -> Unit = {}
    var onAlphaTabStatusCallback: (String) -> Unit = {}
    var onTickPositionCallback: (Long, Boolean) -> Unit = { _, _ -> }
    var onPlaybackProgressCallback: (Long, Boolean, Int) -> Unit = { _, _, _ -> }
    var onRestoreAppliedCallback: (Long, Int, Int) -> Unit = { _, _, _ -> }

    @JavascriptInterface fun postAsciiTab(ascii: String) = onAsciiTabCallback(ascii)
    @JavascriptInterface fun postTabAnalysis(json: String) = onTabAnalysisCallback(json)
    @JavascriptInterface fun postCompactTabs(tabs: String) = onCompactTabsCallback(tabs)
    @JavascriptInterface fun onJsReady() = onJsReadyCallback.invoke()
    @JavascriptInterface fun onScoreLoaded(totalMeasures: Int) = onScoreLoadedCallback(totalMeasures)
    @JavascriptInterface fun onDetectedTempo(bpm: Int) = onDetectedTempoCallback(bpm)
    @JavascriptInterface fun onAlphaTabStatus(message: String) = onAlphaTabStatusCallback(message)
    @JavascriptInterface fun onTickPosition(tick: Long, isPlaying: Boolean) = onTickPositionCallback(tick, isPlaying)
    @JavascriptInterface fun onPlaybackProgress(tick: Long, isPlaying: Boolean, barIndex: Int) =
        onPlaybackProgressCallback(tick, isPlaying, barIndex)
    @JavascriptInterface fun onRestoreApplied(tick: Long, currentBarIndex: Int, requestedBarIndex: Int) =
        onRestoreAppliedCallback(tick, currentBarIndex, requestedBarIndex)
}

private data class TabWebViewEntry(
    val webView: WebView,
    val bridge: TabJsBridge,
    var jsReady: Boolean = false,
    var loadedTotalMeasures: Int = 0,
    var soundFontLoaded: Boolean = false
)

private object SharedTabWebViewPool {
    private const val MAX_ENTRIES = 4
    private val entries = LinkedHashMap<String, TabWebViewEntry>(8, 0.75f, true)

    @Synchronized
    fun get(fileName: String): TabWebViewEntry? = entries[fileName]

    @Synchronized
    fun put(fileName: String, entry: TabWebViewEntry) {
        entries[fileName] = entry
        while (entries.size > MAX_ENTRIES) {
            val eldestKey = entries.entries.firstOrNull()?.key ?: break
            val evicted = entries.remove(eldestKey)
            runCatching { evicted?.webView?.destroy() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabViewerScreen(
    lessonId: String,
    viewModelFactory: ViewModelFactory,
    mainViewModel: MainViewModel,
    onBack: () -> Unit
) {
    val viewModel: TabViewerViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()
    val themeViewModel: ThemeViewModel = viewModel(factory = viewModelFactory)
    val themeUiState by themeViewModel.uiState.collectAsState()
    
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    LaunchedEffect(Unit) {
        mainViewModel.setShowBottomBar(false)
    }

    BackHandler {
        mainViewModel.setShowBottomBar(true)
        onBack()
    }

    LaunchedEffect(lessonId) {
        viewModel.loadLesson(lessonId)
    }

    uiState.lesson?.let {
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
    var silentMode by remember { mutableStateOf(false) }
    val aiSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val notesSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val loopSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val tabScaleOverrides = remember { mutableStateMapOf<String, Float>() }

    Scaffold(
        modifier = Modifier,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { 
                        mainViewModel.setShowBottomBar(true)
                        onBack() 
                    }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back_arrow))
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        if (uiState.lesson == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(modifier = Modifier.padding(padding)) {
                var isPracticeMode by remember { mutableStateOf(false) }
                var currentSpeed by remember(isPracticeMode) { mutableStateOf(if (isPracticeMode) themeUiState.practiceSpeed else themeUiState.normalSpeed) }
                val defaultScale = if (isPracticeMode) themeUiState.practiceTabScale else themeUiState.normalTabScale
                var currentScale by remember(isPracticeMode, uiState.lesson?.id, defaultScale) {
                    mutableStateOf(
                        uiState.lesson?.id?.let { tabScaleOverrides[it] } ?: defaultScale
                    )
                }
                Column(modifier = Modifier.fillMaxSize()) {
                    // ─── Mode Toggle ───────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
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
                                    .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
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
                                Text(label, color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    var isPlaying by remember { mutableStateOf(false) }

                    // ─── Tab Viewer ────────────────────────────
                    TabViewer(
                        fileName = uiState.lesson!!.tabsGpPath,
                        tabBytesBase64 = uiState.tabBytesBase64,
                        soundFontBase64 = uiState.soundFontBase64,
                        tabTitle = uiState.lesson!!.title,
                        isPracticeMode = isPracticeMode,
                        currentSpeed = currentSpeed,
                        onSpeedChange = { currentSpeed = it },
                        tabDisplayMode = themeUiState.tabDisplayMode,
                        onTabDisplayModeChange = { mode -> themeViewModel.setTabDisplayMode(mode) },
                        lastTickPosition = uiState.lastTickPosition,
                        lastBarIndex = uiState.lastBarIndex,
                        restoreTickPosition = uiState.restoreTickPosition,
                        restoreBarIndex = uiState.restoreBarIndex,
                        totalBars = uiState.totalBars,
                        restorePending = uiState.restorePending,
                        onRestoreApplied = { tick, currentBar, requestedBar ->
                            viewModel.onRestoreApplied(tick, currentBar, requestedBar)
                        },
                        wasPlaying = uiState.wasPlaying,
                        onTickPosition = { tick, playing -> viewModel.updatePlaybackState(tick, playing) },
                        onPlaybackProgress = { tick, playing, barIndex, totalBars ->
                            val lesson = uiState.lesson
                            if (lesson != null) {
                                viewModel.updatePlaybackProgress(
                                    lessonId = lesson.id,
                                    lessonTitle = lesson.title,
                                    tick = tick,
                                    barIndex = barIndex,
                                    totalBars = totalBars
                                )
                            }
                            viewModel.updatePlaybackState(tick, playing)
                        },
                        themeUiState = themeUiState,
                        isPlaying = isPlaying,
                        onPlayStateChange = { isPlaying = it },
                        onAsciiTabGenerated = { ascii -> viewModel.setAsciiTab(ascii) },
                        onTabAnalysis = { analysis -> viewModel.setTabAnalysis(analysis) },
                        onCompactTabsGenerated = { tabs -> viewModel.setCompactTabs(tabs) },
                        currentScale = currentScale,
                        onScaleChange = { scale ->
                            currentScale = scale
                            uiState.lesson?.id?.let { tabScaleOverrides[it] = scale }
                        },
                        silentMode = silentMode,
                        onSilentModeChange = { silentMode = it },
                        onOpenAiAssistant = { showAiSheet = true },
                        onOpenNotes = { showNotesSheet = true },
                        onOpenLoop = { showLoopSheet = true },
                        loopStartMeasure = loopStartMeasure,
                        loopEndMeasure = loopEndMeasure,
                        isLoopEnabled = isLoopEnabled,
                        onTotalMeasuresLoaded = { measures ->
                            totalMeasures = measures
                            if (loopEndMeasure == 1 && measures > 1) {
                                loopEndMeasure = measures
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(top = 16.dp, start = 8.dp, end = 8.dp)
                    )

                    // ─── Analysis View (Practice Mode Only) ────
                    androidx.compose.animation.AnimatedVisibility(visible = isPracticeMode) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            GuitarFretboard(
                                analysis = uiState.tabAnalysis,
                                isPlaying = isPlaying,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
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
                                lesson = uiState.lesson!!,
                                viewModelFactory = viewModelFactory,
                                asciiTab = uiState.asciiTab,
                                compactTabs = uiState.compactTabs,
                                totalMeasures = totalMeasures
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
                            onAddAudioNote = { uri -> viewModel.addAudioNoteFromFile(uiState.lesson!!.id, uri) },
                            onRecordAudio = { viewModel.onRecordAudio(uiState.lesson!!.id) },
                            onDeleteAudioNote = { id -> viewModel.deleteAudioNote(id) },
                            onPlayAudio = { note -> viewModel.onPlayAudio(note) },
                            onSeekAudio = { id, prog -> viewModel.onSeekAudio(id, prog) },
                            onAddTextNote = { content -> viewModel.addTextNote(uiState.lesson!!.id, content) },
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
                                onToggleLoop = { isLoopEnabled = it }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun TabViewer(
    fileName: String,
    tabBytesBase64: String?,
    soundFontBase64: String?,
    tabTitle: String,
    isPracticeMode: Boolean,
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    tabDisplayMode: TabDisplayMode,
    onTabDisplayModeChange: (TabDisplayMode) -> Unit,
    lastTickPosition: Long?,
    lastBarIndex: Int?,
    restoreTickPosition: Long?,
    restoreBarIndex: Int?,
    totalBars: Int?,
    restorePending: Boolean,
    onRestoreApplied: (Long, Int, Int) -> Unit,
    wasPlaying: Boolean,
    onTickPosition: (Long, Boolean) -> Unit,
    onPlaybackProgress: (Long, Boolean, Int, Int) -> Unit,
    currentScale: Float,
    onScaleChange: (Float) -> Unit,
    silentMode: Boolean,
    onSilentModeChange: (Boolean) -> Unit,
    onOpenAiAssistant: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenLoop: () -> Unit,
    themeUiState: com.example.thetest1.presentation.main.ThemeUiState,
    isPlaying: Boolean,
    onPlayStateChange: (Boolean) -> Unit,
    onAsciiTabGenerated: (String) -> Unit,
    onTabAnalysis: (String) -> Unit,
    onCompactTabsGenerated: (String) -> Unit,
    loopStartMeasure: Int,
    loopEndMeasure: Int,
    isLoopEnabled: Boolean,
    onTotalMeasuresLoaded: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val restoreTag = "TabRestoreFlow"
    val loadTag = "TabLoadPerf"
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeUiState.themeMode) {
        com.example.thetest1.presentation.main.ThemeMode.DARK -> true
        com.example.thetest1.presentation.main.ThemeMode.LIGHT -> false
        com.example.thetest1.presentation.main.ThemeMode.SYSTEM -> isSystemDark
    }
    val assetLoader = remember {
        WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
            .build()
    }
    val webEntry = remember(fileName) {
        SharedTabWebViewPool.get(fileName) ?: run {
            val webView = WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                setBackgroundColor(if (isDark) android.graphics.Color.parseColor("#1c1b1f") else android.graphics.Color.WHITE)

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
                        Log.d("WebViewConsole", "${consoleMessage?.message()}")
                        return true
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(view: WebView?, request: android.webkit.WebResourceRequest?): android.webkit.WebResourceResponse? {
                        val url = request?.url ?: return null
                        return assetLoader.shouldInterceptRequest(url)
                    }
                    override fun onPageFinished(view: WebView?, url: String?) {}
                }
            }
            val bridge = TabJsBridge()
            webView.addJavascriptInterface(bridge, "Android")
            webView.loadUrl("https://appassets.androidplatform.net/assets/tab_viewer.html")
            val created = TabWebViewEntry(webView = webView, bridge = bridge)
            SharedTabWebViewPool.put(fileName, created)
            created
        }
    }
    var isReady by remember(fileName) { mutableStateOf(webEntry.jsReady) }
    var isScoreLoaded by remember(fileName) { mutableStateOf(webEntry.loadedTotalMeasures > 0) }
    var totalBars by remember { mutableStateOf(0) }
    var loadedSourceForCurrentLesson by remember { mutableStateOf<String?>(null) }
    var loadRequestAtMs by remember { mutableStateOf(0L) }
    var jsReadyAtMs by remember { mutableStateOf(0L) }
    var showDisplaySheet by remember { mutableStateOf(false) }
    var showLearningSheet by remember { mutableStateOf(false) }
    var metronomeEnabled by remember { mutableStateOf(false) }
    var metronomeBpm by remember { mutableStateOf(90) }
    var metronomeBpmTouched by remember { mutableStateOf(false) }
    val displaySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val learningSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    val webView = webEntry.webView

    LaunchedEffect(Unit) {
        runCatching {
            webView.onResume()
            webView.resumeTimers()
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
                Log.d(loadTag, "onJsReady file=$fileName")
                isReady = true
            }
        }
        bridge.onScoreLoadedCallback = { totalMeasures ->
            Handler(Looper.getMainLooper()).post {
                isScoreLoaded = true
                totalBars = totalMeasures
                webEntry.loadedTotalMeasures = totalMeasures
                val now = System.currentTimeMillis()
                val fromRequest = if (loadRequestAtMs > 0L) now - loadRequestAtMs else -1L
                val fromJsReady = if (jsReadyAtMs > 0L) now - jsReadyAtMs else -1L
                Log.d(
                    loadTag,
                    "onScoreLoaded file=$fileName bars=$totalMeasures fromRequestMs=$fromRequest fromJsReadyMs=$fromJsReady source=$loadedSourceForCurrentLesson"
                )
                Log.d(restoreTag, "onScoreLoaded(totalMeasures=$totalMeasures)")
                onTotalMeasuresLoaded(totalMeasures)
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
            Log.d("AlphaTabStatus", message)
            if (message.contains("soundFontLoaded")) {
                webEntry.soundFontLoaded = true
            }
        }
        bridge.onTickPositionCallback = { tick, playing -> onTickPosition(tick, playing) }
        bridge.onPlaybackProgressCallback = { tick, playing, barIndex -> onPlaybackProgress(tick, playing, barIndex, totalBars) }
        bridge.onRestoreAppliedCallback = { tick, currentBarIndex, requestedBarIndex ->
            Log.d(
                restoreTag,
                "JS onRestoreApplied(tick=$tick currentBar=$currentBarIndex requestedBar=$requestedBarIndex)"
            )
            onRestoreApplied(tick, currentBarIndex, requestedBarIndex)
        }
        onDispose {}
    }

    DisposableEffect(Unit) {
        onDispose {
            webView.evaluateJavascript("window.stopAudio();", null)
            runCatching {
                webView.onPause()
                webView.pauseTimers()
            }
        }
    }

    LaunchedEffect(fileName) {
        val canReuseRendered =
            webEntry.jsReady &&
                webEntry.loadedTotalMeasures > 0
        if (canReuseRendered) {
            loadedSourceForCurrentLesson = "reused-webview"
            isScoreLoaded = true
            isReady = true
            totalBars = webEntry.loadedTotalMeasures
            onTotalMeasuresLoaded(webEntry.loadedTotalMeasures)
            Log.d(loadTag, "file change -> REUSE rendered score file=$fileName bars=${webEntry.loadedTotalMeasures}")
        } else {
            loadedSourceForCurrentLesson = null
            isScoreLoaded = false
            Log.d(
                loadTag,
                "file change -> COLD load file=$fileName entry={jsReady=${webEntry.jsReady}, bars=${webEntry.loadedTotalMeasures}}"
            )
        }
        metronomeBpmTouched = false
        loadRequestAtMs = 0L
        jsReadyAtMs = 0L
        Log.d(loadTag, "file change -> reset timing markers file=$fileName")
    }

    // Load SoundFont + score only once per lesson to avoid late resets of cursor after restore.
    LaunchedEffect(fileName, tabBytesBase64, soundFontBase64, isReady, loadedSourceForCurrentLesson) {
        if (isReady) {
            webView.evaluateJavascript("window.setTheme($isDark);", null)
            webView.evaluateJavascript("window.setPracticeModeLayout($isPracticeMode);", null)
            if (loadedSourceForCurrentLesson == null) {
                isScoreLoaded = false
                val soundFont = soundFontBase64
                if (soundFont != null) {
                    if (!webEntry.soundFontLoaded) {
                        Log.d(restoreTag, "loading soundfont before score")
                        webView.evaluateJavascript("window.loadSoundFontFromBase64('$soundFont');", null)
                    } else {
                        Log.d(loadTag, "skip soundfont reload (already loaded in reused WebView)")
                    }
                }
                val base64 = tabBytesBase64
                val isAbsoluteLocalFile = fileName.startsWith("/")
                val isBundledAssetRelative = !fileName.contains("://") && !isAbsoluteLocalFile
                if (isBundledAssetRelative) {
                    val assetUrl = "https://appassets.androidplatform.net/assets/$fileName"
                    loadedSourceForCurrentLesson = "asset-url"
                    loadRequestAtMs = System.currentTimeMillis()
                    Log.d(restoreTag, "loading score from asset url")
                    Log.d(loadTag, "request load assetUrl=$assetUrl")
                    webView.evaluateJavascript("window.loadTab('$assetUrl');", null)
                } else if (base64 != null) {
                    loadedSourceForCurrentLesson = "base64"
                    loadRequestAtMs = System.currentTimeMillis()
                    Log.d(restoreTag, "loading score from base64")
                    Log.d(loadTag, "request load base64 file=$fileName len=${base64.length}")
                    webView.evaluateJavascript("window.loadTabFromBase64('$base64');", null)
                } else if (isAbsoluteLocalFile) {
                    // Fallback only. In WebView appassets context local file:// may be blocked on some devices.
                    loadedSourceForCurrentLesson = "file-fallback"
                    loadRequestAtMs = System.currentTimeMillis()
                    Log.d(restoreTag, "loading score from file fallback")
                    Log.d(loadTag, "request load file fallback path=$fileName")
                    webView.evaluateJavascript("window.loadTab('$fileName');", null)
                } else {
                    Log.d(restoreTag, "waiting for base64 bytes, skip file fallback for bundled tab path: $fileName")
                    Log.d(loadTag, "waiting for tab bytes file=$fileName")
                }
            }
        }
    }

    // React to practice mode toggle (after restore is finished to avoid cursor reset race)
    LaunchedEffect(isPracticeMode, isReady, restorePending) {
        if (isReady && !restorePending) {
            webView.evaluateJavascript("window.setPracticeModeLayout($isPracticeMode);", null)
        }
    }

    // React to currentSpeed changes explicitly
    LaunchedEffect(currentSpeed, isReady) {
        if (isReady) {
            webView.evaluateJavascript("window.setPlaybackSpeed($currentSpeed);", null)
        }
    }

    LaunchedEffect(currentScale, isReady, restorePending) {
        if (isReady && !restorePending) {
            webView.evaluateJavascript("window.setTabScale($currentScale);", null)
        }
    }

    LaunchedEffect(tabDisplayMode, isReady, restorePending) {
        if (isReady && !restorePending) {
            val mode = when (tabDisplayMode) {
                TabDisplayMode.NOTES_ONLY -> "Score"
                TabDisplayMode.TAB_ONLY -> "Tab"
                TabDisplayMode.TAB_AND_NOTES -> "ScoreTab"
            }
            webView.evaluateJavascript("window.setTabDisplayMode('$mode');", null)
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
            Log.d(
                restoreTag,
                "restore effect start: tick=$tick barIndex=$barIndex play=$playFlag isScoreLoaded=$isScoreLoaded"
            )
            Log.d(restoreTag, "restore request sent")
            webView.evaluateJavascript("window.setRestorePlayback($tick, $playFlag, $barIndex);", null)
        }
    }

    LaunchedEffect(isReady) {
        if (isReady) {
            while (true) {
                webView.evaluateJavascript("window.getPlaybackState();") { json ->
                    try {
                        val obj = JSONObject(json)
                        val tick = obj.optLong("tick", 0L)
                        val playing = obj.optBoolean("playing", false)
                        onTickPosition(tick, playing)
                    } catch (_: Exception) {
                    }
                }
                delay(500)
            }
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

    // Re-apply theme if user switches dark/light mode while screen is open
    LaunchedEffect(isDark) {
        webView.setBackgroundColor(if (isDark) android.graphics.Color.parseColor("#1c1b1f") else android.graphics.Color.WHITE)
        if (isReady) {
            webView.evaluateJavascript("window.setTheme($isDark);", null)
        }
    }

    Box(modifier = modifier) {
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
                        webView
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { view -> 
                        view.setBackgroundColor(if (isDark) android.graphics.Color.parseColor("#1c1b1f") else android.graphics.Color.WHITE)
                    }
                )
                
                // Loading overlay
                val isReusedSession = loadedSourceForCurrentLesson == "reused-webview"
                val alpha by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (isScoreLoaded) 0f else 1f,
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = if (isReusedSession) 120 else 500
                    )
                )
                if (alpha > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 8.dp) // Added padding to prevent overlap with tabs/title
                            .background(if (isDark) Color(0xFF1C1B1F) else Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                RoundControlButton(
                    onClick = {
                        webView.evaluateJavascript("window.playPause();", null)
                        onPlayStateChange(!isPlaying)
                    },
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
                        onClick = { showDisplaySheet = true },
                        icon = Icons.Filled.Visibility,
                        contentDescription = stringResource(R.string.display_controls),
                        backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                        iconTint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    RoundControlButton(
                        onClick = { showLearningSheet = true },
                        icon = Icons.Filled.School,
                        contentDescription = stringResource(R.string.learning_controls),
                        backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                        iconTint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }

    if (showDisplaySheet) {
        ModalBottomSheet(
            onDismissRequest = { showDisplaySheet = false },
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
            onDismissRequest = { showLearningSheet = false },
            sheetState = learningSheetState
        ) {
            LearningControlsSheet(
                onOpenAiAssistant = {
                    showLearningSheet = false
                    onOpenAiAssistant()
                },
                onOpenNotes = {
                    showLearningSheet = false
                    onOpenNotes()
                },
                onOpenLoop = {
                    showLearningSheet = false
                    onOpenLoop()
                },
                metronomeEnabled = metronomeEnabled,
                metronomeBpm = metronomeBpm,
                onMetronomeEnabledChange = { metronomeEnabled = it },
                onMetronomeBpmChange = {
                    metronomeBpmTouched = true
                    metronomeBpm = it
                }
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
    onToggleLoop: (Boolean) -> Unit
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
