package com.guitarlearning.presentation.notes

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.guitarlearning.R
import com.guitarlearning.domain.model.AudioNote
import com.guitarlearning.domain.model.TextNote
import com.guitarlearning.data.audio_notes.media.PlayerState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File

internal enum class NotesFilterType {
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
    val showAudioSection = filter != NotesFilterType.TEXT &&
        (showAudioInput || filteredAudioNotes.isNotEmpty() || filter == NotesFilterType.AUDIO)
    val showTextSection = filter != NotesFilterType.AUDIO &&
        (showTextInput || filteredTextNotes.isNotEmpty() || filter == NotesFilterType.TEXT)

    val pickAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let(onAddAudioNote)
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
                NotesSectionHeader(title = stringResource(R.string.audio_notes))
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
                NotesSectionHeader(title = stringResource(R.string.text_notes))
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
            title = { Text(stringResource(R.string.delete_audio_note_title)) },
            text = { Text(stringResource(R.string.delete_audio_note_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAudioNote(note.id)
                        audioNoteToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { audioNoteToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    textNoteToDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { textNoteToDelete = null },
            title = { Text(stringResource(R.string.delete_text_note_title)) },
            text = { Text(stringResource(R.string.delete_text_note_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTextNote(note)
                        textNoteToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { textNoteToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
internal fun NotesFilterType.label(): String {
    return when (this) {
        NotesFilterType.ALL -> stringResource(R.string.filter_all)
        NotesFilterType.TEXT -> stringResource(R.string.notes_filter_text)
        NotesFilterType.AUDIO -> stringResource(R.string.notes_filter_audio)
        NotesFilterType.FAVORITES -> stringResource(R.string.favorites)
    }
}
