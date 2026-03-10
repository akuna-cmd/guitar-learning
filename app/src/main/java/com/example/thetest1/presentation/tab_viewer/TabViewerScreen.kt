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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
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

    LaunchedEffect(lessonId) {
        viewModel.loadLesson(lessonId)
    }

    uiState.lesson?.let {
        LaunchedEffect(it) {
            mainViewModel.setActiveTab(it.id, it.title)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(uiState.lesson?.title ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back_arrow)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        if (uiState.lesson == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(modifier = Modifier.padding(padding)) {
                if (uiState.selectedTabIndex != -1) {
                    ScrollableTabRow(
                        selectedTabIndex = uiState.selectedTabIndex,
                        edgePadding = 16.dp,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = {}
                    ) {
                        uiState.tabs.forEach { tab ->
                            Tab(
                                selected = uiState.selectedTab == tab,
                                onClick = { viewModel.selectTab(tab) },
                                text = {
                                    val text = when (tab) {
                                        LessonTab.THEORY -> stringResource(id = R.string.theory)
                                        LessonTab.TABS -> stringResource(id = R.string.tabs)
                                        LessonTab.AI_ASSISTANT -> stringResource(id = R.string.ai_assistant)
                                        LessonTab.NOTES -> stringResource(id = R.string.notes)
                                    }
                                    Text(text)
                                }
                            )
                        }
                    }
                }

                when (uiState.selectedTab) {
                    LessonTab.THEORY -> TheoryScreen(text = uiState.lesson!!.text)
                    LessonTab.TABS -> {
                        var isPracticeMode by remember { mutableStateOf(false) }
                        Column(modifier = Modifier.fillMaxSize()) {
                            // ─── Mode Toggle ───────────────────────────
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                val modes = listOf(false to "Звичайна гра", true to "Режим розбору")
                                modes.forEach { (practice, label) ->
                                    val selected = isPracticeMode == practice
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable { isPracticeMode = practice }
                                            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Text(label, color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // ─── Tab Viewer ────────────────────────────
                            TabViewer(
                                fileName = uiState.lesson!!.tabsGpPath,
                                isPracticeMode = isPracticeMode,
                                themeUiState = themeUiState,
                                onAsciiTabGenerated = { ascii -> viewModel.setAsciiTab(ascii) },
                                onTabAnalysis = { analysis -> viewModel.setTabAnalysis(analysis) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .let { if (isPracticeMode) it.height(250.dp) else it.weight(1f) }
                            )

                            // ─── Analysis View (Practice Mode Only) ────
                            androidx.compose.animation.AnimatedVisibility(visible = isPracticeMode) {
                                TabAnalysisView(
                                    analysis = uiState.tabAnalysis,
                                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                                )
                            }
                        }
                    }
                    LessonTab.AI_ASSISTANT -> AiAssistantScreen(
                        lesson = uiState.lesson!!,
                        viewModelFactory = viewModelFactory,
                        asciiTab = uiState.asciiTab
                    )
                    LessonTab.NOTES -> NotesScreen(
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
    }
}

@Composable
fun TabAnalysisView(analysis: TabAnalysis?, modifier: Modifier = Modifier) {
    if (analysis == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Торкніться ноти для аналізу", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    Column(modifier = modifier.padding(16.dp)) {
        Text("Аналіз такту ${analysis.barIndex}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            FingerSection(stringResource(R.string.left_hand), analysis.leftHand, Modifier.weight(1f))
            Spacer(modifier = Modifier.width(12.dp))
            FingerSection(stringResource(R.string.right_hand), analysis.rightHand, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(stringResource(R.string.technique), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                analysis.instructions.forEachIndexed { index, instruction ->
                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text("${index + 1}.", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(instruction, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun FingerSection(title: String, fingers: List<FingerInfo>, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(10.dp)) {
                fingers.forEach { finger ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        Box(
                            modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(finger.color))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(finger.finger, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(finger.fingerName, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            val detail = if (finger.fret != null) "${finger.string} • ${finger.fret} лад" else "${finger.string} • ${finger.direction}"
                            Text(detail, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun TabViewer(
    fileName: String,
    isPracticeMode: Boolean,
    themeUiState: com.example.thetest1.presentation.main.ThemeUiState,
    onAsciiTabGenerated: (String) -> Unit,
    onTabAnalysis: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeUiState.themeMode) {
        com.example.thetest1.presentation.main.ThemeMode.DARK -> true
        com.example.thetest1.presentation.main.ThemeMode.LIGHT -> false
        com.example.thetest1.presentation.main.ThemeMode.SYSTEM -> isSystemDark
    }
    var isPlaying  by remember { mutableStateOf(false) }
    var isReady    by remember { mutableStateOf(false) }
    var isScoreLoaded by remember { mutableStateOf(false) }
    var currentSpeed by remember { mutableStateOf(1f) }
    
    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                allowFileAccessFromFileURLs = true
                allowUniversalAccessFromFileURLs = true
            }
            
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    Log.d("WebViewConsole", "${consoleMessage?.message()}")
                    return true
                }
            }

            addJavascriptInterface(object {
                @JavascriptInterface
                fun postAsciiTab(ascii: String) { onAsciiTabGenerated(ascii) }
                @JavascriptInterface
                fun postTabAnalysis(json: String) { onTabAnalysis(json) }
                @JavascriptInterface
                fun onJsReady() {
                    Handler(Looper.getMainLooper()).post { isReady = true }
                }
                @JavascriptInterface
                fun onScoreLoaded() {
                    Handler(Looper.getMainLooper()).post { isScoreLoaded = true }
                }
            }, "Android")
            
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    view?.evaluateJavascript("window.loadTab('$fileName');", null)
                }
            }
            loadUrl("file:///android_asset/tab_viewer.html")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView.evaluateJavascript("window.stopAudio();", null)
            webView.destroy()
        }
    }

    // Apply theme (and load tab) whenever page is ready or theme/file changes
    LaunchedEffect(fileName, isReady) {
        if (isReady) {
            webView.evaluateJavascript("window.setTheme($isDark);", null)
            webView.evaluateJavascript("window.loadTab('$fileName');", null)
            val initialSpeed = if (isPracticeMode) themeUiState.practiceSpeed else themeUiState.normalSpeed
            currentSpeed = initialSpeed
        }
    }

    // React to practice mode toggle
    LaunchedEffect(isPracticeMode, isReady) {
        if (isReady) {
            currentSpeed = if (isPracticeMode) themeUiState.practiceSpeed else themeUiState.normalSpeed
        }
    }

    // React to currentSpeed changes explicitly
    LaunchedEffect(currentSpeed, isReady) {
        if (isReady) {
            webView.evaluateJavascript("window.setPlaybackSpeed($currentSpeed);", null)
        }
    }

    // Re-apply theme if user switches dark/light mode while screen is open
    LaunchedEffect(isDark) {
        webView.setBackgroundColor(if (isDark) android.graphics.Color.parseColor("#1c1b1f") else android.graphics.Color.WHITE)
        if (isReady) webView.evaluateJavascript("window.setTheme($isDark);", null)
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
                    factory = { webView },
                    modifier = Modifier.fillMaxSize(),
                    update = { view -> 
                        view.setBackgroundColor(if (isDark) android.graphics.Color.parseColor("#1c1b1f") else android.graphics.Color.WHITE)
                    }
                )
                
                // Loading overlay
                val alpha by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (isScoreLoaded) 0f else 1f,
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 500)
                )
                if (alpha > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(alpha)
                            .background(if (isDark) Color(0xFF1C1B1F) else Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // ─── Playback controls ────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Play / Pause
                IconButton(onClick = {
                    webView.evaluateJavascript("window.playPause();", null)
                    isPlaying = !isPlaying
                }) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null
                    )
                }

                // Speed chips
                val speeds = listOf(0.25f to "0.25×", 0.5f to "0.5×", 0.75f to "0.75×", 1f to "1×", 1.5f to "1.5×", 2f to "2×")
                speeds.forEach { (speed, label) ->
                    val selected = currentSpeed == speed
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clickable {
                                currentSpeed = speed
                            }
                            .background(
                                color = if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

    }
}
