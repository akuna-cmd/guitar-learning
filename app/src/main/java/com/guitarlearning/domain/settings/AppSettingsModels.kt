package com.guitarlearning.domain.settings

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class TabDisplayMode { TAB_ONLY, TAB_AND_NOTES, NOTES_ONLY }

enum class AppLanguage(val languageTag: String) { UKRAINIAN("uk"), ENGLISH("en") }

enum class AiProvider { GEMINI, LOCAL_LLAMA_CPP }

data class AppSettingsSnapshot(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appLanguage: AppLanguage = AppLanguage.UKRAINIAN,
    val aiProvider: AiProvider = AiProvider.GEMINI,
    val localAiServerUrl: String = "",
    val normalSpeed: Float = 1.0f,
    val practiceSpeed: Float = 0.25f,
    val normalTabScale: Float = 1.0f,
    val practiceTabScale: Float = 1.0f,
    val tabDisplayMode: TabDisplayMode = TabDisplayMode.TAB_AND_NOTES,
    val updatedAt: Long = 0L
)
