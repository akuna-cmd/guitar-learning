package com.example.thetest1.presentation.main

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ThemeUiState(
    val isDarkTheme: Boolean = false,
    val isLoading: Boolean = true
)

class ThemeViewModel(private val dataStore: DataStore<Preferences>) : ViewModel() {

    private val isDarkThemeKey = booleanPreferencesKey("is_dark_theme")

    val uiState: StateFlow<ThemeUiState> = dataStore.data
        .map { preferences ->
            ThemeUiState(
                isDarkTheme = preferences[isDarkThemeKey] ?: false,
                isLoading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeUiState()
        )

    fun toggleTheme() {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                val currentTheme = preferences[isDarkThemeKey] ?: false
                preferences[isDarkThemeKey] = !currentTheme
            }
        }
    }
}