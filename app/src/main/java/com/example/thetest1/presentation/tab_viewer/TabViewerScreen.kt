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
                        Column(modifier = Modifier.fillMaxSize()) {
                            TabViewer(
                                fileName = uiState.lesson!!.tabsGpPath,
                                onAsciiTabGenerated = { ascii -> viewModel.setAsciiTab(ascii) },
                                onTabAnalysis = { analysis -> viewModel.setTabAnalysis(analysis) },
                                modifier = Modifier.fillMaxWidth().height(350.dp)
                            )
                            
                            TabAnalysisView(
                                analysis = uiState.tabAnalysis,
                                modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                            )
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
    onAsciiTabGenerated: (String) -> Unit,
    onTabAnalysis: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var isReady by remember { mutableStateOf(false) }
    
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

    LaunchedEffect(fileName, isReady) {
        if (isReady) {
            webView.evaluateJavascript("window.loadTab('$fileName');", null)
        }
    }

    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = {
                webView.evaluateJavascript("window.playPause();", null)
                isPlaying = !isPlaying
            }) {
                Icon(imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = null)
            }
            AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize().weight(1f))
        }

        if (!isReady) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}
