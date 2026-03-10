package com.example.thetest1.presentation.tab_viewer

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(modifier = Modifier.padding(padding)) {
                if (uiState.selectedTabIndex != -1) {
                    PrimaryTabRow(selectedTabIndex = uiState.selectedTabIndex) {
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
                        TabViewer(
                            fileName = uiState.lesson!!.tabsGpPath,
                            onAsciiTabGenerated = { ascii -> viewModel.setAsciiTab(ascii) },
                            modifier = Modifier.fillMaxSize()
                        )
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
                        onAddAudioNote = { uri ->
                            viewModel.addAudioNoteFromFile(
                                uiState.lesson!!.id,
                                uri
                            )
                        },
                        onRecordAudio = { viewModel.onRecordAudio(uiState.lesson!!.id) },
                        onDeleteAudioNote = { audioNoteId -> viewModel.deleteAudioNote(audioNoteId) },
                        onPlayAudio = { audioNote -> viewModel.onPlayAudio(audioNote) },
                        onSeekAudio = { trackId, progress ->
                            viewModel.onSeekAudio(
                                trackId,
                                progress
                            )
                        },
                        onAddTextNote = { content ->
                            viewModel.addTextNote(
                                uiState.lesson!!.id,
                                content
                            )
                        },
                        onUpdateTextNote = { textNote -> viewModel.updateTextNote(textNote) },
                        onDeleteTextNote = { textNote -> viewModel.deleteTextNote(textNote) }
                    )
                }
            }
        }
    }
}

class WebAppInterface(
    private val onAsciiTabGenerated: (String) -> Unit
) {
    @JavascriptInterface
    fun postAsciiTab(ascii: String) {
        onAsciiTabGenerated(ascii)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun TabViewer(
    fileName: String,
    onAsciiTabGenerated: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var isReady by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val contentAlpha by animateFloatAsState(
        targetValue = if (isReady) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "contentAlpha"
    )

    LaunchedEffect(fileName) {
        isReady = false
        isPlaying = false
    }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .alpha(contentAlpha),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(
                onClick = {
                    webView?.evaluateJavascript("window.playPause();", null)
                    isPlaying = !isPlaying
                }
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(id = if (isPlaying) R.string.pause else R.string.play)
                )
            }

            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            allowFileAccess = true
                            allowContentAccess = true
                            allowFileAccessFromFileURLs = true
                            allowUniversalAccessFromFileURLs = true
                            builtInZoomControls = true
                            displayZoomControls = false
                        }

                        addJavascriptInterface(WebAppInterface(onAsciiTabGenerated), "Android")
                        setBackgroundColor(0x00000000)

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                view?.evaluateJavascript("window.loadTab('$fileName');") {
                                    view.postDelayed({ isReady = true }, 200)
                                }
                            }
                        }
                        loadUrl("file:///android_asset/tab_viewer.html")
                        webView = this
                    }
                },
                update = { }
            )
        }

        AnimatedVisibility(
            visible = !isReady,
            exit = fadeOut(animationSpec = tween(500)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            CircularProgressIndicator()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.destroy()
        }
    }
}
