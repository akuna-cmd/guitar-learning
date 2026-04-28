package com.guitarlearning.presentation.notes

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.guitarlearning.R
import com.guitarlearning.domain.model.AudioNote
import com.guitarlearning.domain.model.TextNote
import com.guitarlearning.presentation.audio_notes.PlayerState
import com.guitarlearning.presentation.ui.formatDurationShort
import com.guitarlearning.presentation.ui.theme.appBlockBorder
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

private enum class NotesFilterType {
    ALL,
    TEXT,
    AUDIO,
    FAVORITES
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NotesScreen(
    audioNotes: List<AudioNote>,
    textNotes: List<TextNote>,
    isRecording: Boolean,
    playerState: PlayerState,
    onAddAudioNote: (Uri) -> Unit,
    onRecordAudio: () -> Unit,
    onDeleteAudioNote: (Int) -> Unit,
    onPlayAudio: (AudioNote) -> Unit,
    onSeekAudio: (String, Float) -> Unit,
    onAddTextNote: (String) -> Unit,
    onUpdateTextNote: (TextNote) -> Unit,
    onDeleteTextNote: (TextNote) -> Unit,
    onRenameAudioNote: (AudioNote, String) -> Unit,
    onToggleAudioFavorite: (AudioNote) -> Unit,
    onToggleTextFavorite: (TextNote) -> Unit,
    showAudioInput: Boolean = true,
    showTextInput: Boolean = true
) {
    var newTextNote by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(NotesFilterType.ALL) }
    var audioNoteToDelete by remember { mutableStateOf<AudioNote?>(null) }
    var textNoteToDelete by remember { mutableStateOf<TextNote?>(null) }
    val recordAudioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    val normalizedQuery = query.trim().lowercase()
    val filteredAudioNotes = remember(audioNotes, normalizedQuery, filter) {
        audioNotes.filter { note ->
            val matchesType = filter == NotesFilterType.ALL ||
                filter == NotesFilterType.AUDIO ||
                (filter == NotesFilterType.FAVORITES && note.isFavorite)
            val matchesQuery = normalizedQuery.isBlank() ||
                File(note.filePath).name.lowercase().contains(normalizedQuery)
            matchesType && matchesQuery
        }
    }
    val filteredTextNotes = remember(textNotes, normalizedQuery, filter) {
        textNotes.filter { note ->
            val matchesType = filter == NotesFilterType.ALL ||
                filter == NotesFilterType.TEXT ||
                (filter == NotesFilterType.FAVORITES && note.isFavorite)
            val matchesQuery = normalizedQuery.isBlank() ||
                note.content.lowercase().contains(normalizedQuery)
            matchesType && matchesQuery
        }
    }
    val showAudioSection = filter != NotesFilterType.TEXT && (showAudioInput || filteredAudioNotes.isNotEmpty() || filter == NotesFilterType.AUDIO)
    val showTextSection = filter != NotesFilterType.AUDIO && (showTextInput || filteredTextNotes.isNotEmpty() || filter == NotesFilterType.TEXT)

    val pickAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let { onAddAudioNote(it) }
        }
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SearchNotesField(
                value = query,
                onValueChange = { query = it }
            )
        }
        item {
            NotesFilterRow(
                selected = filter,
                onSelected = { filter = it }
            )
        }

        if (showAudioSection) {
            item {
                NotesSectionHeader(
                    title = stringResource(R.string.audio_notes)
                )
            }
            if (showAudioInput) {
                item {
                    AudioActionsRow(
                        isRecording = isRecording,
                        onPickAudio = { pickAudioLauncher.launch("audio/*") },
                        onRecordAudio = {
                            if (recordAudioPermissionState.status.isGranted) {
                                onRecordAudio()
                            } else {
                                recordAudioPermissionState.launchPermissionRequest()
                            }
                        }
                    )
                }
            }
            if (showAudioInput) {
                item {
                    AnimatedVisibility(visible = isRecording) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.recording_in_progress),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            if (filteredAudioNotes.isEmpty()) {
                item {
                    EmptyNotesSection(message = stringResource(R.string.no_audio_notes_yet))
                }
            } else {
                items(filteredAudioNotes, key = { "audio_${it.id}" }) { note ->
                    AudioNoteCard(
                        audioNote = note,
                        playerState = playerState,
                        onPlay = { onPlayAudio(note) },
                        onSeek = { progress -> onSeekAudio(note.id.toString(), progress) },
                        onRename = { newName -> onRenameAudioNote(note, newName) },
                        onToggleFavorite = { onToggleAudioFavorite(note) },
                        onDelete = { audioNoteToDelete = note }
                    )
                }
            }
        }

        if (showTextSection) {
            item {
                NotesSectionHeader(
                    title = stringResource(R.string.text_notes)
                )
            }
            if (showTextInput) {
                item {
                    TextComposer(
                        value = newTextNote,
                        onValueChange = { newTextNote = it },
                        onSend = {
                            if (newTextNote.isNotBlank()) {
                                onAddTextNote(newTextNote)
                                newTextNote = ""
                            }
                        }
                    )
                }
            }
            if (filteredTextNotes.isEmpty()) {
                item {
                    EmptyNotesSection(message = stringResource(R.string.no_text_notes_yet))
                }
            } else {
                items(filteredTextNotes, key = { "text_${it.id}" }) { note ->
                    TextNoteCard(
                        note = note,
                        onUpdate = onUpdateTextNote,
                        onDelete = { textNoteToDelete = it },
                        onToggleFavorite = { onToggleTextFavorite(note) }
                    )
                }
            }
        }

        if (!showAudioSection && !showTextSection) {
            item {
                EmptyNotesSection(message = stringResource(R.string.no_notes_yet))
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    audioNoteToDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { audioNoteToDelete = null },
            title = { Text(stringResource(id = R.string.delete_audio_note_title)) },
            text = { Text(stringResource(id = R.string.delete_audio_note_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAudioNote(note.id)
                        audioNoteToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(id = R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { audioNoteToDelete = null }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }

    textNoteToDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { textNoteToDelete = null },
            title = { Text(stringResource(id = R.string.delete_text_note_title)) },
            text = { Text(stringResource(id = R.string.delete_text_note_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTextNote(note)
                        textNoteToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(id = R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { textNoteToDelete = null }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun SearchNotesField(
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = { Icon(Icons.Default.FilterList, contentDescription = null) },
        placeholder = { Text(stringResource(R.string.notes_search_placeholder)) },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun NotesFilterRow(
    selected: NotesFilterType,
    onSelected: (NotesFilterType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        NotesFilterType.entries.forEach { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelected(filter) },
                label = { Text(filter.label()) },
                leadingIcon = {
                    when (filter) {
                        NotesFilterType.ALL -> Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                        NotesFilterType.TEXT -> Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        NotesFilterType.AUDIO -> Icon(Icons.Default.AudioFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        NotesFilterType.FAVORITES -> Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            )
        }
    }
}

@Composable
private fun NotesSectionHeader(
    title: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun AudioActionsRow(
    isRecording: Boolean,
    onPickAudio: () -> Unit,
    onRecordAudio: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GradientActionCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Add,
            title = stringResource(R.string.add_audio_file),
            onClick = onPickAudio
        )
        ActionCard(
            modifier = Modifier.weight(1f),
            icon = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
            title = stringResource(if (isRecording) R.string.stop_recording else R.string.record_audio),
            onClick = onRecordAudio
        )
    }
}

@Composable
private fun TextComposer(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.add_a_note)) },
        shape = RoundedCornerShape(22.dp),
        trailingIcon = {
            IconButton(onClick = onSend) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.add_note),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        )
    )
}

@Composable
private fun AudioNoteCard(
    audioNote: AudioNote,
    playerState: PlayerState,
    onPlay: () -> Unit,
    onSeek: (Float) -> Unit,
    onRename: (String) -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    val trackState = playerState.trackStates[audioNote.id.toString()]
    val isPlaying = playerState.currentPlayingId == audioNote.id.toString() && playerState.isPlaying
    val progress = trackState?.progress ?: 0f
    val duration = trackState?.duration ?: 0
    val currentPosition = (duration * progress).toLong()
    val dateText = remember(audioNote.createdAt) {
        SimpleDateFormat("dd.MM.yyyy • HH:mm", Locale.getDefault()).format(audioNote.createdAt)
    }
    var menuExpanded by remember { mutableStateOf(false) }
    var renameDialogVisible by remember { mutableStateOf(false) }
    var renameDraft by remember(audioNote.filePath) {
        mutableStateOf(File(audioNote.filePath).nameWithoutExtension)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = appBlockBorder()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = File(audioNote.filePath).name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildString {
                            append(dateText)
                            if (duration > 0) {
                                append(" • ")
                                append(formatDurationShort(duration.toLong()))
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (audioNote.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = null,
                        tint = if (audioNote.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.menu))
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.rename)) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                renameDialogVisible = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete)) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = onPlay,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Slider(
                        value = progress,
                        onValueChange = onSeek,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatDurationShort(currentPosition),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatDurationShort(duration.toLong()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (renameDialogVisible) {
        AlertDialog(
            onDismissRequest = { renameDialogVisible = false },
            title = { Text(stringResource(R.string.rename)) },
            text = {
                OutlinedTextField(
                    value = renameDraft,
                    onValueChange = { renameDraft = it },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    label = { Text(stringResource(R.string.new_name)) }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRename(renameDraft)
                        renameDialogVisible = false
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { renameDialogVisible = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun TextNoteCard(
    note: TextNote,
    onUpdate: (TextNote) -> Unit,
    onDelete: (TextNote) -> Unit,
    onToggleFavorite: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var draft by remember(note.id, note.content) { mutableStateOf(note.content) }
    val dateText = remember(note.createdAt) {
        SimpleDateFormat("dd.MM.yyyy • HH:mm", Locale.getDefault()).format(note.createdAt)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = appBlockBorder()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (isEditing) {
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    onUpdate(note.copy(content = draft))
                                    isEditing = false
                                }
                            ) {
                                Text(stringResource(R.string.save))
                            }
                            TextButton(onClick = {
                                draft = note.content
                                isEditing = false
                            }) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    } else {
                        Text(
                            text = note.content,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (!isEditing) {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (note.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = null,
                            tint = if (note.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.menu))
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.edit)) },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    isEditing = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete)) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onDelete(note)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GradientActionCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        border = appBlockBorder(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ActionCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        border = appBlockBorder(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun EmptyNotesSection(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = appBlockBorder()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun NotesFilterType.label(): String {
    return when (this) {
        NotesFilterType.ALL -> stringResource(R.string.filter_all)
        NotesFilterType.TEXT -> stringResource(R.string.notes_filter_text)
        NotesFilterType.AUDIO -> stringResource(R.string.notes_filter_audio)
        NotesFilterType.FAVORITES -> stringResource(R.string.favorites)
    }
}
