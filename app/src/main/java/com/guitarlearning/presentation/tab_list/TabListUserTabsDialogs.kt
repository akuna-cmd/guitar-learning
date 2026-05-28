package com.guitarlearning.presentation.tab_list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.guitarlearning.R
import com.guitarlearning.domain.model.TabItem
import com.guitarlearning.presentation.ui.displayTabFolder

@Composable
internal fun RenameTabDialog(
    tab: TabItem?,
    newName: String,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (TabItem) -> Unit
) {
    tab ?: return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_tab)) },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.new_name)) },
                shape = RoundedCornerShape(16.dp)
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(tab) }) {
                Text(stringResource(R.string.save))
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
internal fun DeleteTabDialog(
    tab: TabItem?,
    onDismiss: () -> Unit,
    onConfirm: (TabItem) -> Unit
) {
    tab ?: return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_tab_title)) },
        text = { Text(stringResource(R.string.delete_tab_message)) },
        confirmButton = {
            Button(
                onClick = { onConfirm(tab) },
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
internal fun MoveTabDialog(
    tab: TabItem?,
    availableFolders: List<String>,
    newFolderName: String,
    onFolderNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onMoveToFolder: (TabItem, String) -> Unit,
    onCreateAndMove: (TabItem) -> Unit
) {
    val context = LocalContext.current
    tab ?: return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tab_list_move_to)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (availableFolders.isEmpty()) {
                    Text(
                        text = stringResource(R.string.tab_list_no_folders_yet),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LazyColumn(modifier = Modifier.heightIn(max = 180.dp)) {
                    items(availableFolders) { folder ->
                        AssistChip(
                            onClick = { onMoveToFolder(tab, folder) },
                            label = { Text(displayTabFolder(context, folder)) },
                            leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = onFolderNameChange,
                    label = { Text(stringResource(R.string.tab_list_new_folder)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onCreateAndMove(tab) }) {
                Text(stringResource(R.string.tab_list_create_and_move))
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
internal fun FolderDialogs(
    showCreateFolderDialog: Boolean,
    newFolderValue: String,
    onNewFolderValueChange: (String) -> Unit,
    onDismissCreate: () -> Unit,
    onCreate: () -> Unit,
    folderToRename: String?,
    renameFolderValue: String,
    onRenameFolderValueChange: (String) -> Unit,
    onDismissRename: () -> Unit,
    onRename: (String) -> Unit,
    folderToDelete: String?,
    noFolderLabel: String,
    onDismissDelete: () -> Unit,
    onDelete: (String) -> Unit
) {
    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = onDismissCreate,
            title = { Text(stringResource(R.string.tab_list_create_folder_title)) },
            text = {
                OutlinedTextField(
                    value = newFolderValue,
                    onValueChange = onNewFolderValueChange,
                    label = { Text(stringResource(R.string.tab_list_folder_name)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
            },
            confirmButton = {
                Button(onClick = onCreate) {
                    Text(stringResource(R.string.tab_list_create))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissCreate) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    folderToRename?.let { folder ->
        AlertDialog(
            onDismissRequest = onDismissRename,
            title = { Text(stringResource(R.string.tab_list_rename_folder_title)) },
            text = {
                OutlinedTextField(
                    value = renameFolderValue,
                    onValueChange = onRenameFolderValueChange,
                    label = { Text(stringResource(R.string.tab_list_new_folder_name)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
            },
            confirmButton = {
                Button(onClick = { onRename(folder) }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRename) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    folderToDelete?.let { folder ->
        AlertDialog(
            onDismissRequest = onDismissDelete,
            title = { Text(stringResource(R.string.tab_list_delete_folder_title)) },
            text = { Text(stringResource(R.string.tab_list_delete_folder_message, noFolderLabel)) },
            confirmButton = {
                Button(
                    onClick = { onDelete(folder) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDelete) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
