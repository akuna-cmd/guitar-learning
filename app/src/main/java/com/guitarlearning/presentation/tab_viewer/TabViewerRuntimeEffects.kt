package com.guitarlearning.presentation.tab_viewer

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

@Composable
internal fun BindTabViewerBridge(
    bridge: TabJsBridge,
    webEntry: TabWebViewEntry,
    fileName: String,
    tabId: String,
    totalBars: Int,
    metronomeBpmTouched: Boolean,
    loadedSourceForCurrentLesson: String?,
    loadRequestAtMs: Long,
    jsReadyAtMs: Long,
    tabViewModel: TabViewerViewModel,
    onAsciiTabGenerated: (String) -> Unit,
    onTabAnalysis: (String) -> Unit,
    onCompactTabsGenerated: (String) -> Unit,
    onTickPosition: (Long, Boolean) -> Unit,
    onPlaybackProgress: (Long, Boolean, Int, Int) -> Unit,
    onRestoreApplied: (Long, Int, Int) -> Unit,
    onLoopIterationCompleted: () -> Unit,
    onTotalMeasuresLoaded: (Int) -> Unit,
    setIsReady: (Boolean) -> Unit,
    setIsScoreLoaded: (Boolean) -> Unit,
    setJsReadyAtMs: (Long) -> Unit,
    setTotalBars: (Int) -> Unit,
    setMetronomeBpm: (Int) -> Unit
) {
    val restoreTag = "TabRestoreFlow"
    val loadTag = "TabLoadPerf"

    DisposableEffect(
        bridge,
        onAsciiTabGenerated,
        onTabAnalysis,
        onCompactTabsGenerated,
        onTickPosition,
        onPlaybackProgress,
        onRestoreApplied,
        onLoopIterationCompleted,
        fileName,
        metronomeBpmTouched,
        totalBars,
        loadedSourceForCurrentLesson,
        loadRequestAtMs,
        jsReadyAtMs
    ) {
        bridge.onAsciiTabCallback = { ascii ->
            Handler(Looper.getMainLooper()).post {
                onAsciiTabGenerated(ascii)
            }
        }
        bridge.onTabAnalysisCallback = { analysis ->
            Handler(Looper.getMainLooper()).post {
                onTabAnalysis(analysis)
            }
        }
        bridge.onCompactTabsCallback = { compactTabs ->
            Handler(Looper.getMainLooper()).post {
                onCompactTabsGenerated(compactTabs)
            }
        }
        bridge.onJsReadyCallback = {
            Handler(Looper.getMainLooper()).post {
                webEntry.jsReady = true
                val now = System.currentTimeMillis()
                setJsReadyAtMs(now)
                if (ENABLE_TAB_PERF_TRACE) {
                    Log.d(loadTag, "onJsReady file=$fileName")
                }
                setIsReady(true)
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
                setIsScoreLoaded(true)
                tabViewModel.markScoreLoaded(totalMeasuresInternal)
                setTotalBars(totalMeasuresInternal)
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
                    setMetronomeBpm(bpm.coerceIn(40, 240))
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
            if (
                message.startsWith("analysisReady") ||
                message.startsWith("analysisNotReady") ||
                message.startsWith("analysisError") ||
                message.startsWith("analysisEmpty")
            ) {
                Handler(Looper.getMainLooper()).post {
                    tabViewModel.markAnalysisLoading(false)
                }
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
        bridge.onLoopIterationCompletedCallback = {
            Handler(Looper.getMainLooper()).post {
                onLoopIterationCompleted()
            }
        }
        onDispose {}
    }
}

@Composable
internal fun DisposeTabViewerWebView(
    bridge: TabJsBridge,
    webView: WebView
) {
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
            bridge.onLoopIterationCompletedCallback = {}
            runCatching { webView.evaluateJavascript("window.stopAudio();", null) }
            runCatching { webView.stopLoading() }
            runCatching { webView.loadUrl("about:blank") }
            runCatching { (webView.parent as? ViewGroup)?.removeView(webView) }
            runCatching { webView.destroy() }
        }
    }
}
