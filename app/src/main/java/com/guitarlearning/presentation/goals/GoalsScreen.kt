package com.guitarlearning.presentation.goals

import androidx.activity.ComponentActivity
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guitarlearning.R
import com.guitarlearning.domain.model.Goal
import com.guitarlearning.domain.model.GoalType
import com.guitarlearning.domain.model.Session
import com.guitarlearning.presentation.main.MainViewModel
import com.guitarlearning.presentation.main.SessionItem
import com.guitarlearning.presentation.notes.NotesScreen
import com.guitarlearning.presentation.tab_viewer.TabNotesViewModel
import com.guitarlearning.presentation.ui.theme.appBlockBorder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val GLOBAL_PROGRESS_NOTES_ID = "__progress_notes__"

private enum class ProgressSection {
    SESSIONS,
    PLANNING,
    NOTES
}

private enum class GoalStatusFilter {
    ALL,
    ACTIVE,
    COMPLETED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen() {
    val activity = LocalContext.current as ComponentActivity
    val viewModel: GoalsViewModel = hiltViewModel()
    val mainViewModel: MainViewModel = hiltViewModel(activity)
    val notesViewModel: TabNotesViewModel = hiltViewModel()

    val uiState by viewModel.uiState.collectAsState()
    val mainUiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val notesUiState by notesViewModel.uiState.collectAsStateWithLifecycle()

    var selectedSection by remember { mutableIntStateOf(0) }
    var goalToDelete by remember { mutableStateOf<Goal?>(null) }
    var statsQuery by remember { mutableStateOf("") }
    var statsDateFilter by remember { mutableStateOf(SessionStatsDateFilter.ALL) }
    var planningQuery by remember { mutableStateOf("") }
    var goalStatusFilter by remember { mutableStateOf(GoalStatusFilter.ALL) }
    LaunchedEffect(Unit) {
        notesViewModel.bindLesson(GLOBAL_PROGRESS_NOTES_ID)
    }

    goalToDelete?.let { goal ->
        AlertDialog(
            onDismissRequest = { goalToDelete = null },
            title = { Text(stringResource(R.string.goal_delete_title)) },
            text = { Text(stringResource(R.string.goal_delete_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteGoal(goal)
                        goalToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { goalToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            if (selectedSection == ProgressSection.PLANNING.ordinal) {
                FloatingActionButton(onClick = viewModel::onAddGoalClicked) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_goal))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedSection) {
                ProgressSection.entries.forEachIndexed { index, section ->
                    Tab(
                        selected = selectedSection == index,
                        onClick = { selectedSection = index },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = section.icon(),
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 1.dp)
                                )
                                Text(
                                    text = section.title(),
                                    maxLines = 1,
                                    softWrap = false,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    )
                }
            }

            when (ProgressSection.entries[selectedSection]) {
                ProgressSection.SESSIONS -> SessionsSection(
                    sessions = mainUiState.sessions,
                    query = statsQuery,
                    onQueryChange = { statsQuery = it },
                    dateFilter = statsDateFilter,
                    onDateFilterChange = { statsDateFilter = it }
                )
                ProgressSection.PLANNING -> PlanningSection(
                    goals = uiState.goals,
                    query = planningQuery,
                    onQueryChange = { planningQuery = it },
                    statusFilter = goalStatusFilter,
                    onStatusFilterChange = { goalStatusFilter = it },
                    onAddGoal = viewModel::onAddGoalClicked,
                    onDelete = { goalToDelete = it },
                    onToggle = viewModel::toggleCustomGoal,
                    onEdit = viewModel::onEditGoalClicked
                )
                ProgressSection.NOTES -> NotesSection(
                    notesViewModel = notesViewModel,
                    uiState = notesUiState
                )
            }

            if (uiState.showAddGoalDialog) {
                AddOrEditGoalDialog(
                    goalToEdit = uiState.goalToEdit,
                    onDismiss = viewModel::onDismissGoalDialog,
                    onConfirm = { goal ->
                        if (uiState.goalToEdit == null) {
                            viewModel.addGoal(goal)
                        } else {
                            viewModel.updateGoal(goal)
                        }
                        viewModel.onDismissGoalDialog()
                    }
                )
            }
        }
    }
}

@Composable
private fun SessionsSection(
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
private fun PlanningSection(
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
private fun NotesSection(
    notesViewModel: TabNotesViewModel,
    uiState: com.guitarlearning.presentation.tab_viewer.TabNotesUiState
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
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(appBlockBorder(), MaterialTheme.shapes.extraLarge),
        colors = CardDefaults.elevatedCardColors()
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

@Composable
private fun GoalItem(
    goal: Goal,
    onDelete: () -> Unit,
    onToggle: () -> Unit,
    onEdit: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(appBlockBorder(), MaterialTheme.shapes.extraLarge)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.description,
                        textDecoration = if (goal.isCompleted) TextDecoration.LineThrough else null,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (goal.type != GoalType.CUSTOM) {
                        Text(
                            text = "${stringResource(R.string.progress)}: ${goal.progress} / ${goal.target}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (goal.type == GoalType.CUSTOM) {
                    IconButton(onClick = onToggle) {
                        Icon(
                            imageVector = if (goal.isCompleted) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                            contentDescription = null
                        )
                    }
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.menu))
                    }
                    androidx.compose.material3.DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit)) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                onEdit()
                                showMenu = false
                            }
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete)) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            onClick = {
                                onDelete()
                                showMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddOrEditGoalDialog(
    goalToEdit: Goal?,
    onDismiss: () -> Unit,
    onConfirm: (Goal) -> Unit
) {
    val context = LocalContext.current
    var selectedGoalType by remember(goalToEdit) { mutableStateOf(goalToEdit?.type ?: GoalType.SESSION_TIME) }
    var description by remember(goalToEdit) { mutableStateOf(goalToEdit?.description ?: "") }
    var target by remember(goalToEdit) { mutableStateOf(goalToEdit?.target?.toString() ?: "") }
    var typeExpanded by remember { mutableStateOf(false) }
    val sessionTimeLabel = stringResource(R.string.session_time)
    val lessonsCompletedLabel = stringResource(R.string.lessons_completed)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (goalToEdit == null) stringResource(R.string.new_goal) else stringResource(R.string.edit_goal)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                androidx.compose.material3.ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { if (goalToEdit == null) typeExpanded = !typeExpanded }
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        readOnly = true,
                        enabled = goalToEdit == null,
                        value = selectedGoalType.label(),
                        onValueChange = {},
                        label = { Text(stringResource(R.string.goal_type)) }
                    )
                    if (goalToEdit == null) {
                        androidx.compose.material3.DropdownMenu(
                            expanded = typeExpanded,
                            onDismissRequest = { typeExpanded = false }
                        ) {
                            GoalType.entries.forEach { type ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(type.label()) },
                                    onClick = {
                                        selectedGoalType = type
                                        typeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                if (selectedGoalType == GoalType.CUSTOM) {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(stringResource(R.string.description)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = target,
                        onValueChange = { target = it.filter(Char::isDigit) },
                        label = {
                            Text(
                                if (selectedGoalType == GoalType.SESSION_TIME) {
                                    stringResource(R.string.target_minutes)
                                } else {
                                    stringResource(R.string.target_lessons)
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val descriptionText = when (selectedGoalType) {
                        GoalType.SESSION_TIME -> sessionTimeLabel
                        GoalType.LESSONS_COMPLETED -> lessonsCompletedLabel
                        GoalType.CUSTOM -> description
                    }
                    val newGoal = goalToEdit?.copy(
                        type = selectedGoalType,
                        description = if (selectedGoalType == GoalType.CUSTOM) description else descriptionText,
                        target = if (selectedGoalType == GoalType.CUSTOM) 1 else target.toIntOrNull() ?: 0,
                        deadline = goalToEdit.deadline
                    ) ?: Goal(
                        type = selectedGoalType,
                        description = descriptionText,
                        target = if (selectedGoalType == GoalType.CUSTOM) 1 else target.toIntOrNull() ?: 0,
                        deadline = System.currentTimeMillis()
                    )
                    onConfirm(newGoal)
                }
            ) {
                Text(if (goalToEdit == null) stringResource(R.string.add) else stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

private enum class SessionStatsDateFilter {
    ALL,
    TODAY,
    WEEK,
    MONTH
}

@Composable
private fun ProgressSection.title(): String {
    return when (this) {
        ProgressSection.SESSIONS -> stringResource(R.string.sessions)
        ProgressSection.PLANNING -> stringResource(R.string.planning)
        ProgressSection.NOTES -> stringResource(R.string.notes)
    }
}

@Composable
private fun ProgressSection.icon() = when (this) {
    ProgressSection.SESSIONS -> Icons.Default.Timer
    ProgressSection.PLANNING -> Icons.Default.Event
    ProgressSection.NOTES -> Icons.Default.InsertDriveFile
}

@Composable
private fun SessionStatsDateFilter.label(): String {
    return when (this) {
        SessionStatsDateFilter.ALL -> stringResource(R.string.filter_all)
        SessionStatsDateFilter.TODAY -> stringResource(R.string.filter_today)
        SessionStatsDateFilter.WEEK -> stringResource(R.string.filter_week)
        SessionStatsDateFilter.MONTH -> stringResource(R.string.filter_month)
    }
}

@Composable
private fun GoalStatusFilter.label(): String {
    return when (this) {
        GoalStatusFilter.ALL -> stringResource(R.string.filter_all)
        GoalStatusFilter.ACTIVE -> stringResource(R.string.goals_filter_active)
        GoalStatusFilter.COMPLETED -> stringResource(R.string.goals_filter_completed)
    }
}

@Composable
private fun GoalType.label(): String {
    return when (this) {
        GoalType.SESSION_TIME -> stringResource(R.string.session_time)
        GoalType.LESSONS_COMPLETED -> stringResource(R.string.lessons_completed)
        GoalType.CUSTOM -> stringResource(R.string.custom)
    }
}
