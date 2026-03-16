package com.example.thetest1.presentation.main

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class ThemeUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val normalSpeed: Float = 1.0f,
    val practiceSpeed: Float = 0.25f,
    val normalTabScale: Float = 1.0f,
    val practiceTabScale: Float = 1.0f,
    val isLoading: Boolean = true
)

class ThemeViewModel(private val dataStore: DataStore<Preferences>) : ViewModel() {

    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val normalSpeedKey = floatPreferencesKey("normal_speed")
    private val practiceSpeedKey = floatPreferencesKey("practice_speed")
    private val normalTabScaleKey = floatPreferencesKey("normal_tab_scale")
    private val practiceTabScaleKey = floatPreferencesKey("practice_tab_scale")

    val uiState: StateFlow<ThemeUiState> = dataStore.data
        .map { preferences ->
            ThemeUiState(
                themeMode = ThemeMode.valueOf(preferences[themeModeKey] ?: ThemeMode.SYSTEM.name),
                normalSpeed = preferences[normalSpeedKey] ?: 1.0f,
                practiceSpeed = preferences[practiceSpeedKey] ?: 0.25f,
                normalTabScale = preferences[normalTabScaleKey] ?: 1.0f,
                practiceTabScale = preferences[practiceTabScaleKey] ?: 1.0f,
                isLoading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeUiState()
        )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[themeModeKey] = mode.name
            }
        }
    }

    fun setNormalSpeed(speed: Float) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[normalSpeedKey] = speed
            }
        }
    }

    fun setPracticeSpeed(speed: Float) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[practiceSpeedKey] = speed
            }
        }
    }

    fun setNormalTabScale(scale: Float) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[normalTabScaleKey] = scale
            }
        }
    }

    fun setPracticeTabScale(scale: Float) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[practiceTabScaleKey] = scale
            }
        }
    }
}
