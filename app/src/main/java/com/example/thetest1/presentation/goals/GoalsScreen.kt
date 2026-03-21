package com.example.thetest1.presentation.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.thetest1.R
import com.example.thetest1.di.ViewModelFactory
import com.example.thetest1.domain.model.Goal
import com.example.thetest1.domain.model.GoalType
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    viewModelFactory: ViewModelFactory
) {
    val viewModel: GoalsViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()
    var goalToDelete by remember { mutableStateOf<Goal?>(null) }

    if (goalToDelete != null) {
        AlertDialog(
            onDismissRequest = { goalToDelete = null },
            title = { Text("Видалити ціль?") },
            text = { Text("Ви впевнені, що хочете видалити цю ціль?") },
            confirmButton = {
                Button(
                    onClick = {
                        goalToDelete?.let { viewModel.deleteGoal(it) }
                        goalToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Видалити")
                }
            },
            dismissButton = {
                TextButton(onClick = { goalToDelete = null }) {
                    Text("Скасувати")
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.onAddGoalClicked() }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_goal))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.showAddGoalDialog) {
                AddOrEditGoalDialog(
                    goalToEdit = uiState.goalToEdit,
                    onDismiss = { viewModel.onDismissGoalDialog() },
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
            if (uiState.goals.isEmpty()) {
                EmptyGoalsState(
                    onAddGoal = { viewModel.onAddGoalClicked() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.goals) { goal ->
                        GoalItem(
                            goal = goal,
                            onDelete = { goalToDelete = goal },
                            onToggle = { viewModel.toggleCustomGoal(goal) },
                            onEdit = { viewModel.onEditGoalClicked(goal) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyGoalsState(
    onAddGoal: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
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
}

@Composable
fun GoalItem(goal: Goal, onDelete: () -> Unit, onToggle: () -> Unit, onEdit: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = goal.description,
                    textDecoration = if (goal.isCompleted) TextDecoration.LineThrough else null,
                    style = MaterialTheme.typography.titleMedium
                )
                if (goal.type != GoalType.CUSTOM) {
                    Text(
                        text = "${stringResource(R.string.progress)}: ${goal.progress} / ${goal.target}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            if (goal.type == GoalType.CUSTOM) {
                IconButton(onClick = onToggle) {
                    Icon(
                        imageVector = if (goal.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = if (goal.isCompleted) stringResource(R.string.completed) else stringResource(
                            R.string.mark_as_completed
                        ),
                        tint = if (goal.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.6f
                        )
                    )
                }
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.menu))
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit)) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            onEdit()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrEditGoalDialog(
    goalToEdit: Goal?,
    onDismiss: () -> Unit,
    onConfirm: (Goal) -> Unit
) {
    var selectedGoalType by remember(goalToEdit) {
        mutableStateOf(
            goalToEdit?.type ?: GoalType.SESSION_TIME
        )
    }
    var description by remember(goalToEdit) { mutableStateOf(goalToEdit?.description ?: "") }
    var target by remember(goalToEdit) { mutableStateOf(goalToEdit?.target?.toString() ?: "") }
    var isExpanded by remember { mutableStateOf(false) }

    val sessionTimeText = stringResource(R.string.session_time)
    val lessonsCompletedText = stringResource(R.string.lessons_completed)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (goalToEdit == null) stringResource(R.string.new_goal) else stringResource(
                    R.string.edit_goal
                )
            )
        },
        text = {
            Column {
                ExposedDropdownMenuBox(
                    expanded = isExpanded,
                    onExpandedChange = { if (goalToEdit == null) isExpanded = !isExpanded }
                ) {
                    OutlinedTextField(
                        modifier = Modifier.menuAnchor(),
                        readOnly = true,
                        value = goalTypeToString(selectedGoalType),
                        onValueChange = {},
                        label = { Text(stringResource(R.string.goal_type)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                        enabled = goalToEdit == null
                    )
                    if (goalToEdit == null) {
                        ExposedDropdownMenu(
                            expanded = isExpanded,
                            onDismissRequest = { isExpanded = false },
                        ) {
                            GoalType.values().forEach { goalType ->
                                DropdownMenuItem(
                                    text = { Text(goalTypeToString(goalType)) },
                                    onClick = {
                                        selectedGoalType = goalType
                                        isExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (selectedGoalType == GoalType.CUSTOM) {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(stringResource(R.string.description)) }
                    )
                } else {
                    OutlinedTextField(
                        value = target,
                        onValueChange = { target = it },
                        label = {
                            Text(
                                if (selectedGoalType == GoalType.SESSION_TIME) stringResource(
                                    R.string.target_minutes
                                ) else stringResource(R.string.target_lessons)
                            )
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val goalDescription = when (selectedGoalType) {
                        GoalType.SESSION_TIME -> sessionTimeText
                        GoalType.LESSONS_COMPLETED -> lessonsCompletedText
                        GoalType.CUSTOM -> description
                    }
                    val today = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    val newGoal = goalToEdit?.copy(
                        type = selectedGoalType,
                        description = if (selectedGoalType == GoalType.CUSTOM && goalToEdit != null) description else goalDescription,
                        target = if (selectedGoalType != GoalType.CUSTOM) target.toIntOrNull()
                            ?: 0 else 1,
                        deadline = today
                    ) ?: Goal(
                        type = selectedGoalType,
                        description = goalDescription,
                        target = if (selectedGoalType != GoalType.CUSTOM) target.toIntOrNull()
                            ?: 0 else 1,
                        deadline = today
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

@Composable
private fun goalTypeToString(goalType: GoalType): String {
    return when (goalType) {
        GoalType.SESSION_TIME -> stringResource(R.string.session_time)
        GoalType.LESSONS_COMPLETED -> stringResource(R.string.lessons_completed)
        GoalType.CUSTOM -> stringResource(R.string.custom)
    }
}
