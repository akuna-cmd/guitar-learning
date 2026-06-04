package com.guitarlearning.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guitarlearning.domain.settings.AiProvider
import com.guitarlearning.domain.settings.AppLanguage
import com.guitarlearning.domain.settings.TabDisplayMode
import com.guitarlearning.domain.settings.ThemeMode
import com.guitarlearning.domain.repository.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ThemeUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appLanguage: AppLanguage = AppLanguage.UKRAINIAN,
    val aiProvider: AiProvider = AiProvider.GEMINI,
    val localAiServerUrl: String = "",
    val normalSpeed: Float = 1.0f,
    val practiceSpeed: Float = 0.3f,
    val normalTabScale: Float = 1.5f,
    val practiceTabScale: Float = 1.5f,
    val tabDisplayMode: TabDisplayMode = TabDisplayMode.TAB_AND_NOTES,
    val isLoading: Boolean = true
)

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository
) : ViewModel() {

    val uiState: StateFlow<ThemeUiState> = appSettingsRepository.observeSettings()
        .map { settings ->
            ThemeUiState(
                themeMode = settings.themeMode,
                appLanguage = settings.appLanguage,
                aiProvider = settings.aiProvider,
                localAiServerUrl = settings.localAiServerUrl,
                normalSpeed = settings.normalSpeed,
                practiceSpeed = settings.practiceSpeed,
                normalTabScale = settings.normalTabScale,
                practiceTabScale = settings.practiceTabScale,
                tabDisplayMode = settings.tabDisplayMode,
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
            appSettingsRepository.setThemeMode(mode)
        }
    }

    fun setAppLanguage(language: AppLanguage) {
        viewModelScope.launch {
            appSettingsRepository.setAppLanguage(language)
        }
    }

    fun setAiProvider(provider: AiProvider) {
        viewModelScope.launch {
            appSettingsRepository.setAiProvider(provider)
        }
    }

    fun setLocalAiServerUrl(url: String) {
        viewModelScope.launch {
            appSettingsRepository.setLocalAiServerUrl(url)
        }
    }

    fun saveAiSettings(provider: AiProvider, url: String) {
        viewModelScope.launch {
            appSettingsRepository.setAiSettings(provider, url)
        }
    }

    fun setNormalSpeed(speed: Float) {
        viewModelScope.launch {
            appSettingsRepository.setNormalSpeed(speed)
        }
    }

    fun setPracticeSpeed(speed: Float) {
        viewModelScope.launch {
            appSettingsRepository.setPracticeSpeed(speed)
        }
    }

    fun setNormalTabScale(scale: Float) {
        viewModelScope.launch {
            appSettingsRepository.setNormalTabScale(scale)
        }
    }

    fun setPracticeTabScale(scale: Float) {
        viewModelScope.launch {
            appSettingsRepository.setPracticeTabScale(scale)
        }
    }

    fun setTabDisplayMode(mode: TabDisplayMode) {
        viewModelScope.launch {
            appSettingsRepository.setTabDisplayMode(mode)
        }
    }

}
