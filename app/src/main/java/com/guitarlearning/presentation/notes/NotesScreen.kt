package com.guitarlearning.presentation.notes

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.guitarlearning.R
import com.guitarlearning.domain.model.AudioNote
import com.guitarlearning.domain.model.TextNote
import com.guitarlearning.presentation.audio_notes.PlayerState
import com.guitarlearning.presentation.ui.theme.appBlockBorder
import com.guitarlearning.presentation.ui.formatDurationShort
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

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
    onDeleteTextNote: (TextNote) -> Unit
) {
    var newTextNote by remember { mutableStateOf("") }
    val recordAudioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    val pickAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let { onAddAudioNote(it) }
        }
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                text = stringResource(id = R.string.audio_notes),
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    onClick = { pickAudioLauncher.launch("audio/*") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(id = R.string.add_audio_file))
                }
                FilledTonalButton(
                    onClick = {
                        if (recordAudioPermissionState.status.isGranted) {
                            onRecordAudio()
                        } else {
                            recordAudioPermissionState.launchPermissionRequest()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isRecording) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (isRecording) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(id = if (isRecording) R.string.stop_recording else R.string.record_audio))
                }
            }
            AnimatedVisibility(visible = isRecording) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(id = R.string.recording_in_progress),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (audioNotes.isEmpty()) {
            item {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    Text(text = stringResource(id = R.string.no_audio_notes_yet))
                }
            }
        } else {
            items(audioNotes, key = { it.id }) { audioNote ->
                AudioNoteItem(
                    audioNote = audioNote,
                    playerState = playerState,
                    onPlay = { onPlayAudio(audioNote) },
                    onSeek = { progress -> onSeekAudio(audioNote.id.toString(), progress) },
                    onDelete = { onDeleteAudioNote(audioNote.id) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(id = R.string.text_notes),
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (textNotes.isEmpty()) {
            item {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    Text(text = stringResource(id = R.string.no_text_notes_yet))
                }
            }
        }

        items(textNotes, key = { it.id }) { note ->
            TextNoteItem(note = note, onUpdate = onUpdateTextNote, onDelete = onDeleteTextNote)
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = newTextNote,
                onValueChange = { newTextNote = it },
                placeholder = { Text(stringResource(id = R.string.add_a_note)) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (newTextNote.isNotBlank()) {
                                onAddTextNote(newTextNote)
                                newTextNote = ""
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = stringResource(id = R.string.add_note),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun AudioNoteItem(
    audioNote: AudioNote,
    playerState: PlayerState,
    onPlay: () -> Unit,
    onSeek: (Float) -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    val trackState = playerState.trackStates[audioNote.id.toString()]
    val isCurrentlyPlaying =
        playerState.currentPlayingId == audioNote.id.toString() && playerState.isPlaying
    val progress = trackState?.progress ?: 0f
    val duration = trackState?.duration ?: 0
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth()
        ,
        border = appBlockBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = File(audioNote.filePath).name,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.delete)) },
                            onClick = {
                                onDelete()
                                menuExpanded = false
                            },
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = progress,
                onValueChange = onSeek,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val currentPosition = (duration * progress).toLong()
                Text(
                    text = if (duration > 0 || isCurrentlyPlaying) formatDurationShort(currentPosition) else "--:--",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = if (duration > 0) formatDurationShort(duration.toLong()) else "--:--",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(onClick = onPlay) {
                    Icon(
                        imageVector = if (isCurrentlyPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = stringResource(id = if (isCurrentlyPlaying) R.string.pause else R.string.play),
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = dateFormat.format(audioNote.createdAt),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun TextNoteItem(
    note: TextNote,
    onUpdate: (TextNote) -> Unit,
    onDelete: (TextNote) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedContent by remember { mutableStateOf(note.content) }
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth()
        ,
        border = appBlockBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (isEditing) {
                OutlinedTextField(
                    value = editedContent,
                    onValueChange = { editedContent = it },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Button(onClick = {
                        onUpdate(note.copy(content = editedContent))
                        isEditing = false
                    }) {
                        Text(stringResource(id = R.string.save))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { isEditing = false }) {
                        Text(stringResource(id = R.string.cancel))
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = note.content)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = dateFormat.format(note.createdAt),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.edit)) },
                                onClick = {
                                    isEditing = true
                                    menuExpanded = false
                                },
                                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.delete)) },
                                onClick = {
                                    onDelete(note)
                                    menuExpanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
