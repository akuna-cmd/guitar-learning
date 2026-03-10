package com.example.thetest1.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.thetest1.R
import com.example.thetest1.di.ViewModelFactory
import com.example.thetest1.presentation.main.ThemeMode
import com.example.thetest1.presentation.main.ThemeViewModel

@Composable
fun SettingsScreen(viewModelFactory: ViewModelFactory) {
    val themeViewModel: ThemeViewModel = viewModel(factory = viewModelFactory)
    val uiState by themeViewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text(
                text = stringResource(id = R.string.settings),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            SettingsSection(title = "Тема застосунку") {
                val modes = listOf(
                    ThemeMode.SYSTEM to "Системна",
                    ThemeMode.LIGHT to "Світла",
                    ThemeMode.DARK to "Темна"
                )
                modes.forEach { (mode, label) ->
                    SettingsOptionsRow(
                        label = label,
                        selected = uiState.themeMode == mode,
                        onClick = { themeViewModel.setThemeMode(mode) }
                    )
                }
            }
        }

        item {
            SettingsSection(title = "Швидкість за замовчуванням (Звичайна гра)") {
                val speeds = listOf(0.5f, 0.75f, 1f, 1.5f, 2f)
                speeds.forEach { speed ->
                    SettingsOptionsRow(
                        label = "${speed}x",
                        selected = uiState.normalSpeed == speed,
                        onClick = { themeViewModel.setNormalSpeed(speed) }
                    )
                }
            }
        }

        item {
            SettingsSection(title = "Швидкість за замовчуванням (Режим розбору)") {
                val speeds = listOf(0.25f, 0.5f, 0.75f, 1f)
                speeds.forEach { speed ->
                    SettingsOptionsRow(
                        label = "${speed}x",
                        selected = uiState.practiceSpeed == speed,
                        onClick = { themeViewModel.setPracticeSpeed(speed) }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsOptionsRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, modifier = Modifier.weight(1f))
        RadioButton(selected = selected, onClick = null)
    }
}
