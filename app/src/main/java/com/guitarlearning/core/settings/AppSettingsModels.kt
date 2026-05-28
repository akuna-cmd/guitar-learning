package com.guitarlearning.core.settings

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class TabDisplayMode { TAB_ONLY, TAB_AND_NOTES, NOTES_ONLY }

enum class FretboardDisplayMode { SIMPLE, DETAILED }

enum class AppLanguage(val languageTag: String) { UKRAINIAN("uk"), ENGLISH("en") }

enum class AiProvider { GEMINI, LOCAL_LLAMA_CPP }
