package com.example.thetest1.presentation.audio_notes

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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.thetest1.R
import com.example.thetest1.domain.model.AudioNote
import com.example.thetest1.presentation.ui.theme.appBlockBorder
import com.example.thetest1.presentation.util.formatDurationShort
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AudioNotesScreen(
    audioNotes: List<AudioNote>,
    isRecording: Boolean,
    playerState: PlayerState,
    onAddAudioNote: (Uri) -> Unit,
    onRecordAudio: () -> Unit,
    onDeleteAudioNote: (Int) -> Unit,
    onPlayAudio: (AudioNote) -> Unit,
    onSeekAudio: (String, Float) -> Unit
) {
    val recordAudioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    val pickAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let { onAddAudioNote(it) }
        }
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { pickAudioLauncher.launch("audio/*") }) {
                Text(stringResource(id = R.string.add_audio_file))
            }
            Button(
                onClick = {
                    if (recordAudioPermissionState.status.isGranted) {
                        onRecordAudio()
                    } else {
                        recordAudioPermissionState.launchPermissionRequest()
                    }
                }
            ) {
                Text(stringResource(id = if (isRecording) R.string.stop_recording else R.string.record_audio))
            }
        }

        AnimatedVisibility(visible = isRecording) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(id = R.string.recording_in_progress))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (audioNotes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = stringResource(id = R.string.no_notes_yet))
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(audioNotes) { audioNote ->
                    AudioNoteItem(
                        audioNote = audioNote,
                        playerState = playerState,
                        onPlay = { onPlayAudio(audioNote) },
                        onSeek = { progress -> onSeekAudio(audioNote.id.toString(), progress) },
                        onDelete = { onDeleteAudioNote(audioNote.id) }
                    )
                }
            }
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
        ,
        border = appBlockBorder()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = File(audioNote.filePath).name,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(id = R.string.delete)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Slider(
                value = progress,
                onValueChange = { newProgress -> onSeek(newProgress) },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val currentPosition = (duration * progress).toLong()
                Text(
                    text = formatDurationShort(currentPosition),
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                )
                Text(
                    text = formatDurationShort(duration.toLong()),
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall
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
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
