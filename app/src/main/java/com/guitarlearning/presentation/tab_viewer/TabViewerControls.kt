package com.guitarlearning.presentation.tab_viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guitarlearning.R
import com.guitarlearning.presentation.main.TabDisplayMode
import com.guitarlearning.presentation.ui.HoldableIconButton
import com.guitarlearning.presentation.ui.formatScale
import com.guitarlearning.presentation.ui.formatSpeed
import com.guitarlearning.presentation.ui.stepScale
import com.guitarlearning.presentation.ui.stepSpeed
import com.guitarlearning.presentation.ui.theme.appBlockBorder
import kotlin.math.roundToInt

private fun stepBpm(value: Int, delta: Int): Int {
    return (value + delta).coerceIn(40, 240)
}

private fun formatSpeedPercent(speed: Float): String = "${(speed * 100f).roundToInt()}%"

private fun stepLoopSpeedPercent(value: Float, deltaPercent: Int): Float {
    val stepped = value + (deltaPercent / 100f)
    return ((stepped * 100f).roundToInt() / 100f).coerceIn(0.05f, 2.5f)
}

private fun stepRepeatCount(value: Int, delta: Int): Int = (value + delta).coerceIn(1, 32)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TabViewerSheets(
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
    onMetronomeBpmChange: (Int) -> Unit,
    trackOptions: List<TabTrackOption>,
    selectedTrackIndex: Int,
    transposeSemitones: Int,
    onTrackSelected: (Int) -> Unit,
    onTransposeChange: (Int) -> Unit
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
                onMetronomeBpmChange = onMetronomeBpmChange,
                trackOptions = trackOptions,
                selectedTrackIndex = selectedTrackIndex,
                transposeSemitones = transposeSemitones,
                onTrackSelected = onTrackSelected,
                onTransposeChange = onTransposeChange
            )
        }
    }
}

@Composable
internal fun RoundControlButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    backgroundColor: Color,
    iconTint: Color
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .border(appBlockBorder(), CircleShape)
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

@Composable
internal fun DisplayControlsSheet(
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
        Text(
            text = stringResource(R.string.display_controls),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        SheetSection {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Speed, contentDescription = null)
                Text(
                    text = stringResource(R.string.speed_value_format, formatSpeed(currentSpeed)),
                    modifier = Modifier.weight(1f)
                )
                HoldableIconButton(
                    onClick = { onSpeedChange(stepSpeed(currentSpeed, -0.1f)) },
                    contentDescription = stringResource(R.string.speed_decrease),
                    icon = Icons.Default.Remove
                )
                HoldableIconButton(
                    onClick = { onSpeedChange(stepSpeed(currentSpeed, 0.1f)) },
                    contentDescription = stringResource(R.string.speed_increase),
                    icon = Icons.Default.Add
                )
            }
        }
        SheetSection {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.ZoomIn, contentDescription = null)
                Text(
                    text = stringResource(R.string.scale_value_format, formatScale(currentScale)),
                    modifier = Modifier.weight(1f)
                )
                HoldableIconButton(
                    onClick = { onScaleChange(stepScale(currentScale, -0.1f)) },
                    contentDescription = stringResource(R.string.scale_decrease),
                    icon = Icons.Default.Remove
                )
                HoldableIconButton(
                    onClick = { onScaleChange(stepScale(currentScale, 0.1f)) },
                    contentDescription = stringResource(R.string.scale_increase),
                    icon = Icons.Default.Add
                )
            }
        }
        Text(
            text = stringResource(R.string.display_mode),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SheetSection {
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
        }
        SheetSection(
            contentPadding = 0.dp,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.silent_mode)) },
                supportingContent = { Text(stringResource(R.string.silent_mode_desc)) },
                trailingContent = {
                    Switch(
                        checked = silentMode,
                        onCheckedChange = onSilentModeChange
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ListItemDefaults.colors(
                    containerColor = Color.Transparent
                )
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
internal fun LearningControlsSheet(
    onOpenAiAssistant: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenLoop: () -> Unit,
    metronomeEnabled: Boolean,
    metronomeBpm: Int,
    onMetronomeEnabledChange: (Boolean) -> Unit,
    onMetronomeBpmChange: (Int) -> Unit,
    trackOptions: List<TabTrackOption>,
    selectedTrackIndex: Int,
    transposeSemitones: Int,
    onTrackSelected: (Int) -> Unit,
    onTransposeChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.learning_controls),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        LearningSheetItem(
            onClick = onOpenNotes,
            leadingIcon = Icons.Filled.NoteAdd,
            headline = stringResource(R.string.notes)
        )
        LearningSheetItem(
            onClick = onOpenLoop,
            leadingIcon = Icons.Filled.Repeat,
            headline = stringResource(R.string.loop_section)
        )
        SheetSection(
            contentPadding = 0.dp,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.track_selection)) },
                    supportingContent = {
                        Text(
                            text = trackOptions.firstOrNull { it.index == selectedTrackIndex }?.name
                                ?: stringResource(R.string.track_selection_empty)
                        )
                    },
                    leadingContent = { Icon(Icons.Filled.QueueMusic, contentDescription = null) },
                    colors = androidx.compose.material3.ListItemDefaults.colors(
                        containerColor = Color.Transparent
                    )
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val fallbackTrackName = stringResource(R.string.track_selection_empty)
                    val visibleTracks = trackOptions.ifEmpty {
                        listOf(TabTrackOption(index = selectedTrackIndex, name = fallbackTrackName))
                    }
                    visibleTracks.forEach { track ->
                        TrackOptionRow(
                            track = track,
                            selected = track.index == selectedTrackIndex,
                            onClick = { onTrackSelected(track.index) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        SheetSection(
            contentPadding = 0.dp,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.transpose_track)) },
                supportingContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.transpose_value, transposeSemitones),
                            modifier = Modifier.weight(1f)
                        )
                        HoldableIconButton(
                            onClick = { onTransposeChange((transposeSemitones - 1).coerceAtLeast(-36)) },
                            contentDescription = stringResource(R.string.transpose_decrease),
                            icon = Icons.Default.Remove
                        )
                        HoldableIconButton(
                            onClick = { onTransposeChange((transposeSemitones + 1).coerceAtMost(36)) },
                            contentDescription = stringResource(R.string.transpose_increase),
                            icon = Icons.Default.Add
                        )
                    }
                },
                leadingContent = { Icon(Icons.Filled.Tune, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ListItemDefaults.colors(
                    containerColor = Color.Transparent
                )
            )
        }
        SheetSection(
            contentPadding = 0.dp,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.metronome)) },
                supportingContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.metronome_bpm, metronomeBpm),
                            modifier = Modifier.weight(1f)
                        )
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
                },
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ListItemDefaults.colors(
                    containerColor = Color.Transparent
                )
            )
        }
        LearningSheetItem(
            onClick = onOpenAiAssistant,
            leadingIcon = Icons.Filled.AutoAwesome,
            headline = stringResource(R.string.ai_assistant)
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun TrackOptionRow(
    track: TabTrackOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.MusicNote,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = track.name,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SheetSection(
    modifier: Modifier = Modifier,
    contentPadding: androidx.compose.ui.unit.Dp = 12.dp,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        border = appBlockBorder()
    ) {
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

@Composable
private fun LearningSheetItem(
    onClick: () -> Unit,
    leadingIcon: ImageVector,
    headline: String
) {
    SheetSection(
        contentPadding = 0.dp,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        ListItem(
            headlineContent = { Text(headline) },
            leadingContent = { Icon(leadingIcon, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            colors = androidx.compose.material3.ListItemDefaults.colors(
                containerColor = Color.Transparent
            )
        )
    }
}

@Composable
internal fun TabDisplayModeMenu(
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
internal fun SpeedScaleMenu(
    icon: ImageVector,
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
        DropdownMenu(
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
internal fun LoopConfigurator(
    totalMeasures: Int,
    startMeasure: Int,
    endMeasure: Int,
    isLoopEnabled: Boolean,
    onStartChange: (Int) -> Unit,
    onEndChange: (Int) -> Unit,
    onToggleLoop: (Boolean) -> Unit,
    isLoopAccelerationEnabled: Boolean,
    loopAccelerationStartSpeed: Float,
    loopAccelerationEndSpeed: Float,
    loopAccelerationStep: Float,
    loopAccelerationRepeats: Int,
    loopAccelerationCompletedRepeats: Int,
    onToggleLoopAcceleration: (Boolean) -> Unit,
    onLoopAccelerationStartSpeedChange: (Float) -> Unit,
    onLoopAccelerationEndSpeedChange: (Float) -> Unit,
    onLoopAccelerationStepChange: (Float) -> Unit,
    onLoopAccelerationRepeatsChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.loop_config_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleLoop(!isLoopEnabled) }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.loop_enable), style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = isLoopEnabled,
                onCheckedChange = { onToggleLoop(it) }
            )
        }
        Spacer(Modifier.height(16.dp))
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.loop_start_measure, startMeasure), fontWeight = FontWeight.Bold)
            androidx.compose.material3.Slider(
                value = startMeasure.toFloat(),
                onValueChange = { onStartChange(it.toInt().coerceAtMost(endMeasure)) },
                valueRange = 1f..totalMeasures.toFloat().coerceAtLeast(1f),
                steps = if (totalMeasures > 2) totalMeasures - 2 else 0
            )
        }
        Spacer(Modifier.height(8.dp))
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.loop_end_measure, endMeasure), fontWeight = FontWeight.Bold)
            androidx.compose.material3.Slider(
                value = endMeasure.toFloat(),
                onValueChange = { onEndChange(it.toInt().coerceAtLeast(startMeasure)) },
                valueRange = 1f..totalMeasures.toFloat().coerceAtLeast(1f),
                steps = if (totalMeasures > 2) totalMeasures - 2 else 0
            )
        }
        Spacer(Modifier.height(16.dp))
        SheetSection(
            contentPadding = 0.dp,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.loop_gradual_speedup_title)) },
                    supportingContent = { Text(stringResource(R.string.loop_gradual_speedup_description)) },
                    leadingContent = { Icon(Icons.Default.Speed, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = isLoopAccelerationEnabled,
                            onCheckedChange = onToggleLoopAcceleration,
                            enabled = isLoopEnabled
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ListItemDefaults.colors(
                        containerColor = Color.Transparent
                    )
                )
                if (isLoopAccelerationEnabled) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LoopValueRow(
                            title = stringResource(R.string.loop_gradual_start_tempo),
                            value = formatSpeedPercent(loopAccelerationStartSpeed),
                            onDecrease = {
                                onLoopAccelerationStartSpeedChange(
                                    stepLoopSpeedPercent(loopAccelerationStartSpeed, -5)
                                        .coerceAtMost(loopAccelerationEndSpeed)
                                )
                            },
                            onIncrease = {
                                onLoopAccelerationStartSpeedChange(
                                    stepLoopSpeedPercent(loopAccelerationStartSpeed, 5)
                                        .coerceAtMost(loopAccelerationEndSpeed)
                                )
                            },
                            decrementDescription = stringResource(R.string.speed_decrease),
                            incrementDescription = stringResource(R.string.speed_increase)
                        )
                        LoopValueRow(
                            title = stringResource(R.string.loop_gradual_end_tempo),
                            value = formatSpeedPercent(loopAccelerationEndSpeed),
                            onDecrease = {
                                onLoopAccelerationEndSpeedChange(
                                    stepLoopSpeedPercent(loopAccelerationEndSpeed, -5)
                                        .coerceAtLeast(loopAccelerationStartSpeed)
                                )
                            },
                            onIncrease = {
                                onLoopAccelerationEndSpeedChange(stepLoopSpeedPercent(loopAccelerationEndSpeed, 5))
                            },
                            decrementDescription = stringResource(R.string.speed_decrease),
                            incrementDescription = stringResource(R.string.speed_increase)
                        )
                        LoopValueRow(
                            title = stringResource(R.string.loop_gradual_step),
                            value = formatSpeedPercent(loopAccelerationStep),
                            onDecrease = {
                                onLoopAccelerationStepChange(
                                    stepLoopSpeedPercent(loopAccelerationStep, -5).coerceAtLeast(0.05f)
                                )
                            },
                            onIncrease = {
                                onLoopAccelerationStepChange(
                                    stepLoopSpeedPercent(loopAccelerationStep, 5).coerceAtMost(1.0f)
                                )
                            },
                            decrementDescription = stringResource(R.string.speed_decrease),
                            incrementDescription = stringResource(R.string.speed_increase)
                        )
                        LoopValueRow(
                            title = stringResource(R.string.loop_gradual_repeats),
                            value = stringResource(R.string.loop_gradual_repeats_value, loopAccelerationRepeats),
                            onDecrease = {
                                onLoopAccelerationRepeatsChange(stepRepeatCount(loopAccelerationRepeats, -1))
                            },
                            onIncrease = {
                                onLoopAccelerationRepeatsChange(stepRepeatCount(loopAccelerationRepeats, 1))
                            },
                            decrementDescription = stringResource(R.string.loop_gradual_repeats_decrease),
                            incrementDescription = stringResource(R.string.loop_gradual_repeats_increase)
                        )
                        Text(
                            text = stringResource(
                                R.string.loop_gradual_progress,
                                loopAccelerationCompletedRepeats,
                                loopAccelerationRepeats
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun LoopValueRow(
    title: String,
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    decrementDescription: String,
    incrementDescription: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        HoldableIconButton(
            onClick = onDecrease,
            contentDescription = decrementDescription,
            icon = Icons.Default.Remove
        )
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        HoldableIconButton(
            onClick = onIncrease,
            contentDescription = incrementDescription,
            icon = Icons.Default.Add
        )
    }
}
