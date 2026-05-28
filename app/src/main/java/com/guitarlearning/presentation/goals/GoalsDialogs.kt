package com.guitarlearning.presentation.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.guitarlearning.R
import com.guitarlearning.domain.model.Goal
import com.guitarlearning.domain.model.GoalType
import com.guitarlearning.presentation.ui.theme.appBlockBorder

@Composable
internal fun GoalDeleteDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.goal_delete_title)) },
        text = { Text(stringResource(R.string.goal_delete_message)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.delete))
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
internal fun GoalItem(
    goal: Goal,
    onDelete: () -> Unit,
    onToggle: () -> Unit,
    onEdit: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(),
        elevation = CardDefaults.elevatedCardElevation(),
        border = appBlockBorder()
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
                            imageVector = if (goal.isCompleted) {
                                Icons.Default.CheckCircle
                            } else {
                                Icons.Outlined.RadioButtonUnchecked
                            },
                            contentDescription = null
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddOrEditGoalDialog(
    goalToEdit: Goal?,
    onDismiss: () -> Unit,
    onConfirm: (Goal) -> Unit
) {
    var selectedGoalType by remember(goalToEdit) { mutableStateOf(goalToEdit?.type ?: GoalType.SESSION_TIME) }
    var description by remember(goalToEdit) { mutableStateOf(goalToEdit?.description ?: "") }
    var target by remember(goalToEdit) { mutableStateOf(goalToEdit?.target?.toString() ?: "") }
    var typeExpanded by remember { mutableStateOf(false) }
    val sessionTimeLabel = stringResource(R.string.session_time)
    val lessonsCompletedLabel = stringResource(R.string.lessons_completed)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (goalToEdit == null) stringResource(R.string.new_goal) else stringResource(R.string.edit_goal))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExposedDropdownMenuBox(
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
                        DropdownMenu(
                            expanded = typeExpanded,
                            onDismissRequest = { typeExpanded = false }
                        ) {
                            GoalType.entries.forEach { type ->
                                DropdownMenuItem(
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
                    val goal = goalToEdit?.copy(
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
                    onConfirm(goal)
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
