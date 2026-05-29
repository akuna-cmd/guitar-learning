package com.guitarlearning.presentation.tab_viewer

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.CenterAlignedTopAppBar
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guitarlearning.R
import com.guitarlearning.presentation.ai_assistant.AiAssistantScreen
import com.guitarlearning.presentation.main.MainViewModel
import com.guitarlearning.presentation.notes.NotesScreen
import com.guitarlearning.presentation.main.ThemeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
private const val DEFAULT_LOOP_ACCELERATION_START_SPEED = 0.5f
private const val DEFAULT_LOOP_ACCELERATION_END_SPEED = 1.5f
private const val DEFAULT_LOOP_ACCELERATION_STEP = 0.25f
private const val DEFAULT_LOOP_ACCELERATION_REPEATS = 1

private fun clampLoopSpeed(value: Float): Float = value.coerceIn(0.1f, 2.5f)
private fun clampLoopStep(value: Float): Float = value.coerceIn(0.05f, 1.0f)

private fun clampLoopRepeatCount(value: Int): Int = value.coerceIn(1, 32)

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
    var isLoopAccelerationEnabled by remember { mutableStateOf(false) }
    var loopAccelerationStartSpeed by remember { mutableStateOf(themeUiState.practiceSpeed) }
    var loopAccelerationEndSpeed by remember { mutableStateOf(DEFAULT_LOOP_ACCELERATION_END_SPEED) }
    var loopAccelerationStep by remember { mutableStateOf(DEFAULT_LOOP_ACCELERATION_STEP) }
    var loopAccelerationRepeats by remember { mutableStateOf(DEFAULT_LOOP_ACCELERATION_REPEATS) }
    var loopAccelerationCompletedRepeats by remember { mutableStateOf(0) }
    var hasInitializedLoopRange by remember { mutableStateOf(false) }
    var silentMode by remember { mutableStateOf(false) }
    val aiSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val notesSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val loopSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val tabScaleOverrides = remember { mutableStateMapOf<String, Float>() }

    LaunchedEffect(lessonId) {
        totalMeasures = 1
        loopStartMeasure = 1
        loopEndMeasure = 1
        isLoopEnabled = false
        isLoopAccelerationEnabled = false
        loopAccelerationStartSpeed = DEFAULT_LOOP_ACCELERATION_START_SPEED
        loopAccelerationEndSpeed = DEFAULT_LOOP_ACCELERATION_END_SPEED
        loopAccelerationStep = DEFAULT_LOOP_ACCELERATION_STEP
        loopAccelerationRepeats = DEFAULT_LOOP_ACCELERATION_REPEATS
        loopAccelerationCompletedRepeats = 0
        hasInitializedLoopRange = false
    }

    val contentAlpha = if (isDisplayUnlocked) 1f else 0f

    Scaffold(
        modifier = Modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = lesson?.title.orEmpty(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
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

                fun resetLoopAccelerationProgress(
                    applyStartSpeed: Boolean = false,
                    startOverride: Float? = null
                ) {
                    loopAccelerationCompletedRepeats = 0
                    if (applyStartSpeed && isLoopEnabled && isLoopAccelerationEnabled) {
                        val targetSpeed = clampLoopSpeed(startOverride ?: loopAccelerationStartSpeed)
                            .coerceAtMost(loopAccelerationEndSpeed)
                        currentSpeed = targetSpeed
                    }
                }
                var previousPlayingState by remember(lesson.id) { mutableStateOf(isPlayingState) }

                LaunchedEffect(isPlayingState, isLoopEnabled, isLoopAccelerationEnabled) {
                    if (isPlayingState && !previousPlayingState && isLoopEnabled && isLoopAccelerationEnabled) {
                        resetLoopAccelerationProgress(applyStartSpeed = true)
                    }
                    previousPlayingState = isPlayingState
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
                                if (!hasInitializedLoopRange && loopEndMeasure == 1 && measures > 1) {
                                    loopEndMeasure = measures
                                    hasInitializedLoopRange = true
                                } else if (!hasInitializedLoopRange) {
                                    hasInitializedLoopRange = true
                                }
                            },
                            onLoopIterationCompleted = {
                                if (isLoopEnabled && isLoopAccelerationEnabled) {
                                    val nextRepeatCount = loopAccelerationCompletedRepeats + 1
                                    loopAccelerationCompletedRepeats = nextRepeatCount
                                    if (nextRepeatCount % loopAccelerationRepeats == 0) {
                                        val nextSpeed = clampLoopSpeed(currentSpeed + loopAccelerationStep)
                                            .coerceAtMost(loopAccelerationEndSpeed)
                                        if (nextSpeed > currentSpeed) {
                                            currentSpeed = nextSpeed
                                        }
                                    }
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
                                onDeleteTextNote = { note -> notesViewModel.deleteTextNote(note) },
                                onRenameAudioNote = notesViewModel::renameAudioNote,
                                onToggleAudioFavorite = notesViewModel::toggleAudioFavorite,
                                onToggleTextFavorite = notesViewModel::toggleTextFavorite
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
                                onStartChange = {
                                    loopStartMeasure = it
                                    resetLoopAccelerationProgress(applyStartSpeed = true)
                                },
                                onEndChange = {
                                    loopEndMeasure = it
                                    resetLoopAccelerationProgress(applyStartSpeed = true)
                                },
                                onToggleLoop = {
                                    isLoopEnabled = it
                                    if (!it) {
                                        isLoopAccelerationEnabled = false
                                        loopAccelerationCompletedRepeats = 0
                                    }
                                    resetLoopAccelerationProgress(applyStartSpeed = it)
                                },
                                isLoopAccelerationEnabled = isLoopAccelerationEnabled,
                                loopAccelerationStartSpeed = loopAccelerationStartSpeed,
                                loopAccelerationEndSpeed = loopAccelerationEndSpeed,
                                loopAccelerationStep = loopAccelerationStep,
                                loopAccelerationRepeats = loopAccelerationRepeats,
                                loopAccelerationCompletedRepeats = loopAccelerationCompletedRepeats % loopAccelerationRepeats,
                                onToggleLoopAcceleration = { enabled ->
                                    if (!isLoopEnabled) return@LoopConfigurator
                                    isLoopAccelerationEnabled = enabled
                                    if (enabled) {
                                        loopAccelerationStartSpeed = DEFAULT_LOOP_ACCELERATION_START_SPEED
                                        loopAccelerationEndSpeed = DEFAULT_LOOP_ACCELERATION_END_SPEED
                                        loopAccelerationStep = DEFAULT_LOOP_ACCELERATION_STEP
                                        loopAccelerationRepeats = DEFAULT_LOOP_ACCELERATION_REPEATS
                                        resetLoopAccelerationProgress(applyStartSpeed = false)
                                    } else {
                                        loopAccelerationCompletedRepeats = 0
                                    }
                                },
                                onLoopAccelerationStartSpeedChange = { updated ->
                                    val sanitized = clampLoopSpeed(updated)
                                    loopAccelerationStartSpeed = sanitized.coerceAtMost(loopAccelerationEndSpeed)
                                    resetLoopAccelerationProgress(applyStartSpeed = true, startOverride = loopAccelerationStartSpeed)
                                },
                                onLoopAccelerationEndSpeedChange = { updated ->
                                    val sanitized = clampLoopSpeed(updated).coerceAtLeast(loopAccelerationStartSpeed)
                                    loopAccelerationEndSpeed = sanitized
                                    if (currentSpeed > sanitized && isLoopAccelerationEnabled) {
                                        currentSpeed = sanitized
                                    }
                                    resetLoopAccelerationProgress(applyStartSpeed = false)
                                },
                                onLoopAccelerationStepChange = { updated ->
                                    loopAccelerationStep = clampLoopStep(updated)
                                    resetLoopAccelerationProgress(applyStartSpeed = false)
                                },
                                onLoopAccelerationRepeatsChange = { updated ->
                                    loopAccelerationRepeats = clampLoopRepeatCount(updated)
                                    resetLoopAccelerationProgress(applyStartSpeed = false)
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
