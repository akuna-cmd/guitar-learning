package com.guitarlearning.presentation.tab_list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.guitarlearning.R
import com.guitarlearning.domain.model.TabItem
import com.guitarlearning.domain.model.isDefaultTabFolder
import com.guitarlearning.presentation.ui.displayTabFolder
import com.guitarlearning.presentation.ui.theme.appBlockBorder

@Composable
internal fun UserTabsToolbar(
    uiState: TabListUiState,
    allFoldersLabel: String,
    onQueryChange: (String) -> Unit,
    onSelectFolder: (String?) -> Unit,
    folderActionsExpanded: Boolean,
    onFolderActionsExpandedChange: (Boolean) -> Unit,
    onShowCreateFolderDialog: () -> Unit,
    onShowRenameFolderDialog: () -> Unit,
    onShowDeleteFolderDialog: () -> Unit,
    sortMenuExpanded: Boolean,
    onSortMenuExpandedChange: (Boolean) -> Unit,
    onSelectSortMode: (UserTabsSortMode) -> Unit
) {
    val context = LocalContext.current
    OutlinedTextField(
        value = uiState.userTabsQuery,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        placeholder = { Text(stringResource(R.string.tab_list_search_placeholder)) },
        shape = RoundedCornerShape(20.dp)
    )
    Spacer(modifier = Modifier.height(12.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilterChip(
                selected = uiState.selectedFolder == null,
                onClick = { onSelectFolder(null) },
                label = { Text(allFoldersLabel) }
            )
            uiState.availableFolders.forEach { folder ->
                FilterChip(
                    selected = uiState.selectedFolder == folder,
                    onClick = { onSelectFolder(folder) },
                    label = { Text(displayTabFolder(context, folder)) }
                )
            }
        }
        Box {
            IconButton(onClick = { onFolderActionsExpandedChange(true) }) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.tab_list_folder_actions))
            }
            DropdownMenu(
                expanded = folderActionsExpanded,
                onDismissRequest = { onFolderActionsExpandedChange(false) }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.tab_list_new_folder)) },
                    leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
                    onClick = {
                        onFolderActionsExpandedChange(false)
                        onShowCreateFolderDialog()
                    }
                )
                if (uiState.selectedFolder != null && !isDefaultTabFolder(uiState.selectedFolder)) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.rename)) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            onFolderActionsExpandedChange(false)
                            onShowRenameFolderDialog()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete)) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        onClick = {
                            onFolderActionsExpandedChange(false)
                            onShowDeleteFolderDialog()
                        }
                    )
                }
            }
        }
        Box {
            IconButton(onClick = { onSortMenuExpandedChange(true) }) {
                Icon(Icons.Default.SwapVert, contentDescription = stringResource(R.string.tab_list_sort))
            }
            DropdownMenu(
                expanded = sortMenuExpanded,
                onDismissRequest = { onSortMenuExpandedChange(false) }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.tab_list_sort_progress)) },
                    onClick = {
                        onSelectSortMode(UserTabsSortMode.PROGRESS_ASC)
                        onSortMenuExpandedChange(false)
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.tab_list_sort_date_added)) },
                    onClick = {
                        onSelectSortMode(UserTabsSortMode.DATE_ADDED_DESC)
                        onSortMenuExpandedChange(false)
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.tab_list_sort_alphabetical)) },
                    onClick = {
                        onSelectSortMode(UserTabsSortMode.ALPHABETICAL_ASC)
                        onSortMenuExpandedChange(false)
                    }
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
internal fun UserTabCard(
    tab: TabItem,
    progress: Int?,
    folderLabel: String,
    onOpen: () -> Unit,
    onMenuExpanded: () -> Unit,
    isMenuExpanded: Boolean,
    onMenuDismiss: () -> Unit,
    onMove: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(),
        elevation = CardDefaults.elevatedCardElevation(),
        border = appBlockBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tab.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Box {
                    IconButton(onClick = onMenuExpanded) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.menu))
                    }
                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = onMenuDismiss
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.tab_list_move_to)) },
                            leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
                            onClick = onMove
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.rename)) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = onRename
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete)) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            onClick = onDelete
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.wrapContentWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = folderLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    LinearProgressIndicator(
                        progress = { ((progress ?: 0).coerceIn(0, 100)) / 100f },
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TabProgressBadge(progressPercent = progress)
                }
            }
        }
    }
}
