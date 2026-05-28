package com.guitarlearning.presentation.goals

import androidx.activity.ComponentActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guitarlearning.R
import com.guitarlearning.domain.model.GoalType
import com.guitarlearning.presentation.main.MainViewModel
import com.guitarlearning.presentation.tab_viewer.TabNotesViewModel

internal const val GLOBAL_PROGRESS_NOTES_ID = "__progress_notes__"

internal enum class ProgressSection {
    SESSIONS,
    PLANNING,
    NOTES
}

internal enum class GoalStatusFilter {
    ALL,
    ACTIVE,
    COMPLETED
}

internal enum class SessionStatsDateFilter {
    ALL,
    TODAY,
    WEEK,
    MONTH
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
    var goalToDelete by remember { mutableStateOf<com.guitarlearning.domain.model.Goal?>(null) }
    var statsQuery by remember { mutableStateOf("") }
    var statsDateFilter by remember { mutableStateOf(SessionStatsDateFilter.ALL) }
    var planningQuery by remember { mutableStateOf("") }
    var goalStatusFilter by remember { mutableStateOf(GoalStatusFilter.ALL) }

    LaunchedEffect(Unit) {
        notesViewModel.bindLesson(GLOBAL_PROGRESS_NOTES_ID)
    }

    goalToDelete?.let { goal ->
        GoalDeleteDialog(
            onConfirm = {
                viewModel.deleteGoal(goal)
                goalToDelete = null
            },
            onDismiss = { goalToDelete = null }
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
            GoalsTabRow(
                selectedSection = selectedSection,
                onSectionSelected = { selectedSection = it }
            )

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
private fun GoalsTabRow(
    selectedSection: Int,
    onSectionSelected: (Int) -> Unit
) {
    TabRow(selectedTabIndex = selectedSection) {
        ProgressSection.entries.forEachIndexed { index, section ->
            Tab(
                selected = selectedSection == index,
                onClick = { onSectionSelected(index) },
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
}

@Composable
internal fun ProgressSection.title(): String {
    return when (this) {
        ProgressSection.SESSIONS -> stringResource(R.string.sessions)
        ProgressSection.PLANNING -> stringResource(R.string.planning)
        ProgressSection.NOTES -> stringResource(R.string.notes)
    }
}

@Composable
internal fun ProgressSection.icon() = when (this) {
    ProgressSection.SESSIONS -> Icons.Default.Timer
    ProgressSection.PLANNING -> Icons.Default.Event
    ProgressSection.NOTES -> Icons.Default.InsertDriveFile
}

@Composable
internal fun SessionStatsDateFilter.label(): String {
    return when (this) {
        SessionStatsDateFilter.ALL -> stringResource(R.string.filter_all)
        SessionStatsDateFilter.TODAY -> stringResource(R.string.filter_today)
        SessionStatsDateFilter.WEEK -> stringResource(R.string.filter_week)
        SessionStatsDateFilter.MONTH -> stringResource(R.string.filter_month)
    }
}

@Composable
internal fun GoalStatusFilter.label(): String {
    return when (this) {
        GoalStatusFilter.ALL -> stringResource(R.string.filter_all)
        GoalStatusFilter.ACTIVE -> stringResource(R.string.goals_filter_active)
        GoalStatusFilter.COMPLETED -> stringResource(R.string.goals_filter_completed)
    }
}

@Composable
internal fun GoalType.label(): String {
    return when (this) {
        GoalType.SESSION_TIME -> stringResource(R.string.session_time)
        GoalType.LESSONS_COMPLETED -> stringResource(R.string.lessons_completed)
        GoalType.CUSTOM -> stringResource(R.string.custom)
    }
}
