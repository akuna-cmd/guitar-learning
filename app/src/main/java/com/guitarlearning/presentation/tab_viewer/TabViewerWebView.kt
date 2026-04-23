package com.guitarlearning.presentation.tab_viewer

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import com.guitarlearning.BuildConfig
import com.guitarlearning.presentation.main.TabDisplayMode
import com.guitarlearning.presentation.main.ThemeUiState

internal const val ENABLE_TAB_PERF_TRACE = false

internal class TabJsBridge {
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

    @JavascriptInterface
    fun onPlaybackProgress(tick: Long, isPlaying: Boolean, barIndex: Int) =
        onPlaybackProgressCallback(tick, isPlaying, barIndex)

    @JavascriptInterface
    fun onRestoreApplied(tick: Long, currentBarIndex: Int, requestedBarIndex: Int) =
        onRestoreAppliedCallback(tick, currentBarIndex, requestedBarIndex)
}

internal data class TabWebViewEntry(
    val webView: WebView,
    val bridge: TabJsBridge,
    var jsReady: Boolean = false,
    var loadedTotalMeasures: Int = 0,
    var soundFontLoaded: Boolean = false,
    var loadedFileName: String? = null,
    var lastKnownTick: Long = 0L,
    var lastKnownBarIndex: Int = 0
)

internal data class TabTrackOption(
    val index: Int,
    val name: String
)

internal fun createTabWebViewEntry(context: Context): TabWebViewEntry {
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

internal data class TabViewerModel(
    val tabId: String,
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

internal data class TabViewerHandlers(
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
    val onTotalMeasuresLoaded: (Int) -> Unit
)
