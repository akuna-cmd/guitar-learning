package com.guitarlearning.presentation.goals

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guitarlearning.R
import com.guitarlearning.domain.model.Goal
import com.guitarlearning.domain.model.Session
import com.guitarlearning.presentation.main.SessionItem
import com.guitarlearning.presentation.notes.NotesScreen
import com.guitarlearning.presentation.tab_viewer.TabNotesUiState
import com.guitarlearning.presentation.tab_viewer.TabNotesViewModel
import com.guitarlearning.presentation.ui.theme.appBlockBorder
import java.util.Calendar
import java.util.concurrent.TimeUnit

@Composable
internal fun SessionsSection(
    sessions: List<Session>,
    query: String,
    onQueryChange: (String) -> Unit,
    dateFilter: SessionStatsDateFilter,
    onDateFilterChange: (SessionStatsDateFilter) -> Unit
) {
    val todayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val tomorrowStart = remember(todayStart) { todayStart + TimeUnit.DAYS.toMillis(1) }
    val filteredSessions = remember(sessions, query, dateFilter, todayStart, tomorrowStart) {
        val now = System.currentTimeMillis()
        val normalizedQuery = query.trim().lowercase()
        sessions.filter { session ->
            val matchesQuery = normalizedQuery.isBlank() || session.practicedTabs.any {
                it.tabName.lowercase().contains(normalizedQuery)
            }
            val matchesDate = when (dateFilter) {
                SessionStatsDateFilter.ALL -> true
                SessionStatsDateFilter.TODAY -> session.startTime.time in todayStart until tomorrowStart
                SessionStatsDateFilter.WEEK -> session.startTime.time >= now - TimeUnit.DAYS.toMillis(7)
                SessionStatsDateFilter.MONTH -> session.startTime.time >= now - TimeUnit.DAYS.toMillis(30)
            }
            matchesQuery && matchesDate
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SearchField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = stringResource(R.string.statistics_search_placeholder)
            )
        }
        item {
            FilterRow(
                labels = SessionStatsDateFilter.entries.map { it.label() },
                selectedIndex = dateFilter.ordinal,
                onSelect = { onDateFilterChange(SessionStatsDateFilter.entries[it]) }
            )
        }
        items(filteredSessions, key = { it.id }) { session ->
            SessionItem(session)
        }
    }
}

@Composable
internal fun PlanningSection(
    goals: List<Goal>,
    query: String,
    onQueryChange: (String) -> Unit,
    statusFilter: GoalStatusFilter,
    onStatusFilterChange: (GoalStatusFilter) -> Unit,
    onAddGoal: () -> Unit,
    onDelete: (Goal) -> Unit,
    onToggle: (Goal) -> Unit,
    onEdit: (Goal) -> Unit
) {
    val filteredGoals = remember(goals, query, statusFilter) {
        val normalizedQuery = query.trim().lowercase()
        goals.filter { goal ->
            val matchesQuery = normalizedQuery.isBlank() || goal.description.lowercase().contains(normalizedQuery)
            val matchesStatus = when (statusFilter) {
                GoalStatusFilter.ALL -> true
                GoalStatusFilter.ACTIVE -> !goal.isCompleted
                GoalStatusFilter.COMPLETED -> goal.isCompleted
            }
            matchesQuery && matchesStatus
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SearchField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = stringResource(R.string.goals_search_placeholder)
            )
        }
        item {
            FilterRow(
                labels = GoalStatusFilter.entries.map { it.label() },
                selectedIndex = statusFilter.ordinal,
                onSelect = { onStatusFilterChange(GoalStatusFilter.entries[it]) }
            )
        }
        if (filteredGoals.isEmpty()) {
            item {
                EmptyGoalsState(onAddGoal = onAddGoal)
            }
        } else {
            items(filteredGoals, key = { it.syncId }) { goal ->
                GoalItem(
                    goal = goal,
                    onDelete = { onDelete(goal) },
                    onToggle = { onToggle(goal) },
                    onEdit = { onEdit(goal) }
                )
            }
        }
    }
}

@Composable
internal fun NotesSection(
    notesViewModel: TabNotesViewModel,
    uiState: TabNotesUiState
) {
    NotesScreen(
        audioNotes = uiState.audioNotes,
        textNotes = uiState.textNotes,
        isRecording = uiState.isRecording,
        playerState = uiState.playerState,
        onAddAudioNote = { uri -> notesViewModel.addAudioNoteFromFile(GLOBAL_PROGRESS_NOTES_ID, uri) },
        onRecordAudio = { notesViewModel.onRecordAudio(GLOBAL_PROGRESS_NOTES_ID) },
        onDeleteAudioNote = { id -> notesViewModel.deleteAudioNote(id) },
        onPlayAudio = notesViewModel::onPlayAudio,
        onSeekAudio = notesViewModel::onSeekAudio,
        onAddTextNote = { content -> notesViewModel.addTextNote(GLOBAL_PROGRESS_NOTES_ID, content) },
        onUpdateTextNote = notesViewModel::updateTextNote,
        onDeleteTextNote = notesViewModel::deleteTextNote,
        onRenameAudioNote = notesViewModel::renameAudioNote,
        onToggleAudioFavorite = notesViewModel::toggleAudioFavorite,
        onToggleTextFavorite = notesViewModel::toggleTextFavorite
    )
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        placeholder = { Text(placeholder) },
        shape = MaterialTheme.shapes.extraLarge
    )
}

@Composable
private fun FilterRow(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        labels.forEachIndexed { index, label ->
            FilterChip(
                selected = selectedIndex == index,
                onClick = { onSelect(index) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun EmptyGoalsState(onAddGoal: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(),
        elevation = CardDefaults.elevatedCardElevation(),
        border = appBlockBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.empty_goals_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.empty_goals_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onAddGoal) {
                Text(stringResource(R.string.add_first_goal))
            }
        }
    }
}
