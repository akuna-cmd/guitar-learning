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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
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

    var showAiSheet by remember { mutableStateOf(false) }
    var showNotesSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back_arrow))
                    }
                },
                actions = {
                    var expandedMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { expandedMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Меню")
                    }
                    androidx.compose.material3.DropdownMenu(
                        expanded = expandedMenu,
                        onDismissRequest = { expandedMenu = false }
                    ) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Нотатки") },
                            leadingIcon = { Icon(Icons.Filled.NoteAdd, contentDescription = null) },
                            onClick = {
                                expandedMenu = false
                                showNotesSheet = true
                            }
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("AI помічник") },
                            leadingIcon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null) },
                            onClick = {
                                expandedMenu = false
                                showAiSheet = true
                            }
                        )
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

                    var isPlaying by remember { mutableStateOf(false) }

                    // ─── Tab Viewer ────────────────────────────
                    TabViewer(
                        fileName = uiState.lesson!!.tabsGpPath,
                        tabTitle = uiState.lesson!!.title,
                        isPracticeMode = isPracticeMode,
                        currentSpeed = currentSpeed,
                        onSpeedChange = { currentSpeed = it },
                        themeUiState = themeUiState,
                        isPlaying = isPlaying,
                        onPlayStateChange = { isPlaying = it },
                        onAsciiTabGenerated = { ascii -> viewModel.setAsciiTab(ascii) },
                        onTabAnalysis = { analysis -> viewModel.setTabAnalysis(analysis) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, start = 8.dp, end = 8.dp)
                            .let { if (isPracticeMode) it.height(200.dp) else it.weight(1f) }
                    )

                    // ─── Analysis View (Practice Mode Only) ────
                    androidx.compose.animation.AnimatedVisibility(visible = isPracticeMode) {
                        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
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
                        sheetState = sheetState,
                        modifier = Modifier.fillMaxHeight(0.9f)
                    ) {
                        AiAssistantScreen(
                            lesson = uiState.lesson!!,
                            viewModelFactory = viewModelFactory,
                            asciiTab = uiState.asciiTab
                        )
                    }
                }

                if (showNotesSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showNotesSheet = false },
                        sheetState = sheetState,
                        modifier = Modifier.fillMaxHeight(0.9f)
                    ) {
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
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun TabViewer(
    fileName: String,
    tabTitle: String,
    isPracticeMode: Boolean,
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    themeUiState: com.example.thetest1.presentation.main.ThemeUiState,
    isPlaying: Boolean,
    onPlayStateChange: (Boolean) -> Unit,
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
    var isReady    by remember { mutableStateOf(false) }
    var isScoreLoaded by remember { mutableStateOf(false) }
    
    val webView = remember(isDark) {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(if (isDark) android.graphics.Color.parseColor("#1c1b1f") else android.graphics.Color.WHITE)
            
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
            webView.evaluateJavascript("window.setPracticeModeLayout($isPracticeMode);", null)
            webView.evaluateJavascript("window.loadTab('$fileName');", null)
        }
    }

    // React to practice mode toggle
    LaunchedEffect(isPracticeMode, isReady) {
        if (isReady) {
            webView.evaluateJavascript("window.setPracticeModeLayout($isPracticeMode);", null)
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
                    onPlayStateChange(!isPlaying)
                }) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null
                    )
                }

                // Speed dropdown
                var expanded by remember { mutableStateOf(false) }
                val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                Box {
                    androidx.compose.material3.OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text("${speedString(currentSpeed)}x", fontWeight = FontWeight.Bold)
                    }
                    androidx.compose.material3.DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        speeds.forEach { speed ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("${speedString(speed)}x") },
                                onClick = {
                                    onSpeedChange(speed)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun speedString(speed: Float): String {
    return if (speed % 1.0f == 0f) {
        String.format("%.1f", speed).replace(',', '.')
    } else {
        speed.toString().replace(',', '.')
    }
}
