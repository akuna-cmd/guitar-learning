package com.guitarlearning.presentation.tab_viewer

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.guitarlearning.domain.settings.TabDisplayMode
import com.guitarlearning.domain.settings.ThemeMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

internal const val TAB_VIEWER_DARK_BACKGROUND = "#1c1b1f"

private const val JS_DISPLAY_MODE_SCORE = "Score"
private const val JS_DISPLAY_MODE_TAB = "Tab"
private const val JS_DISPLAY_MODE_SCORE_TAB = "ScoreTab"

private fun TabDisplayMode.toJsMode(): String = when (this) {
    TabDisplayMode.NOTES_ONLY -> JS_DISPLAY_MODE_SCORE
    TabDisplayMode.TAB_ONLY -> JS_DISPLAY_MODE_TAB
    TabDisplayMode.TAB_AND_NOTES -> JS_DISPLAY_MODE_SCORE_TAB
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun TabViewer(
    model: TabViewerModel,
    handlers: TabViewerHandlers,
    tabViewModel: TabViewerViewModel,
    modifier: Modifier = Modifier
) {
    val tabId = model.tabId
    val fileName = model.fileName
    val tabBytesReady = model.tabBytesReady
    val soundFontReady = model.soundFontReady
    val isPracticeMode = model.isPracticeMode
    val currentSpeed = model.currentSpeed
    val tabDisplayMode = model.tabDisplayMode
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
    val showScoreContent = model.showScoreContent

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
    val onLoopIterationCompleted = handlers.onLoopIterationCompleted

    val restoreTag = "TabRestoreFlow"
    val loadTag = "TabLoadPerf"
    val layoutTag = "TabLayoutTrace"
    val frameTag = "TabFramePerf"
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeUiState.themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemDark
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
    var trackOptions by remember(fileName) { mutableStateOf<List<TabTrackOption>>(emptyList()) }
    var selectedTrackIndex by remember(fileName) { mutableStateOf(0) }
    var transposeSemitones by remember(fileName) { mutableStateOf(0) }
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
    BindTabViewerBridge(
        bridge = bridge,
        webEntry = webEntry,
        fileName = fileName,
        tabId = tabId,
        totalBars = totalBars,
        metronomeBpmTouched = metronomeBpmTouched,
        loadedSourceForCurrentLesson = loadedSourceForCurrentLesson,
        loadRequestAtMs = loadRequestAtMs,
        jsReadyAtMs = jsReadyAtMs,
        tabViewModel = tabViewModel,
        onAsciiTabGenerated = onAsciiTabGenerated,
        onTabAnalysis = onTabAnalysis,
        onCompactTabsGenerated = onCompactTabsGenerated,
        onTickPosition = onTickPosition,
        onPlaybackProgress = onPlaybackProgress,
        onRestoreApplied = onRestoreApplied,
        onLoopIterationCompleted = onLoopIterationCompleted,
        onTotalMeasuresLoaded = onTotalMeasuresLoaded,
        setIsReady = { isReady = it },
        setIsScoreLoaded = { isScoreLoaded = it },
        setJsReadyAtMs = { jsReadyAtMs = it },
        setTotalBars = { totalBars = it },
        setMetronomeBpm = { metronomeBpm = it }
    )
    DisposeTabViewerWebView(bridge = bridge, webView = webView)

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

    fun refreshTrackOptions() {
        if (!isReady || !isScoreLoaded) return
        webView.evaluateJavascript("window.getTrackOptions();") { raw ->
            val parsed = parseTrackOptionsFromJs(raw)
            if (parsed.isNotEmpty()) {
                trackOptions = parsed
                if (parsed.none { it.index == selectedTrackIndex }) {
                    selectedTrackIndex = parsed.first().index
                }
            }
        }
    }

    LaunchedEffect(isReady, isScoreLoaded, fileName) {
        if (isReady && isScoreLoaded) {
            refreshTrackOptions()
        }
    }

    LaunchedEffect(selectedTrackIndex, transposeSemitones, isReady, isScoreLoaded) {
        if (isReady && isScoreLoaded) {
            webView.evaluateJavascript(
                "window.applyLearningTools($selectedTrackIndex, $transposeSemitones);"
            ) {
                refreshTrackOptions()
            }
        }
    }

    LaunchedEffect(isPracticeMode, isReady, isScoreLoaded) {
        if (isPracticeMode && isReady && isScoreLoaded) {
            webView.evaluateJavascript("window.requestFullAnalysis();", null)
        }
    }

    LaunchedEffect(fileName, tabBytesReady, soundFontReady, isReady, loadedSourceForCurrentLesson) {
        if (isReady) {
            val modeStr = tabDisplayMode.toJsMode()
            webView.evaluateJavascript("window.initSettings($isDark, $isPracticeMode, $currentSpeed, $currentScale, '$modeStr', '$jsLanguageTag');", null)
            if (loadedSourceForCurrentLesson == null) {
                isScoreLoaded = false
                tabViewModel.markScoreLoading()
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
                } else {
                    if (ENABLE_TAB_PERF_TRACE) {
                        Log.d(restoreTag, "waiting for base64 bytes for local tab path: $fileName")
                        Log.d(loadTag, "waiting for tab bytes file=$fileName")
                    }
                }
            }
        }
    }

    LaunchedEffect(isPracticeMode, currentSpeed, currentScale, tabDisplayMode, isReady, restorePending, languageTag) {
        if (isReady && !restorePending) {
            val modeStr = tabDisplayMode.toJsMode()
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
        webView.setBackgroundColor(
            if (isDark) android.graphics.Color.parseColor(TAB_VIEWER_DARK_BACKGROUND) else android.graphics.Color.WHITE
        )
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
        showScoreContent = showScoreContent,
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
        onOpenAiAssistant = {
            if (isReady && isScoreLoaded) {
                webView.evaluateJavascript("window.ensureFullAnalysis();") {
                    onOpenAiAssistant()
                }
            } else {
                onOpenAiAssistant()
            }
        },
        onOpenNotes = onOpenNotes,
        onOpenLoop = onOpenLoop,
        metronomeEnabled = metronomeEnabled,
        metronomeBpm = metronomeBpm,
        onMetronomeEnabledChange = { metronomeEnabled = it },
        onMetronomeBpmChange = {
            metronomeBpmTouched = true
            metronomeBpm = it
        },
        trackOptions = trackOptions,
        selectedTrackIndex = selectedTrackIndex,
        transposeSemitones = transposeSemitones,
        onTrackSelected = { selectedTrackIndex = it },
        onTransposeChange = { transposeSemitones = it.coerceIn(-36, 36) }
    )
}
