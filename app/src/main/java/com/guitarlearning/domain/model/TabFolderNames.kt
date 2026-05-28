package com.guitarlearning.domain.model

const val DEFAULT_TAB_FOLDER_KEY = "__default__"
private const val LEGACY_DEFAULT_TAB_FOLDER_UK = "Без папки"

fun normalizeTabFolder(raw: String?): String {
    val trimmed = raw?.trim().orEmpty()
    return if (trimmed.isEmpty() || trimmed == LEGACY_DEFAULT_TAB_FOLDER_UK || trimmed == DEFAULT_TAB_FOLDER_KEY) {
        DEFAULT_TAB_FOLDER_KEY
    } else {
        trimmed
    }
}

fun isDefaultTabFolder(folder: String?): Boolean = normalizeTabFolder(folder) == DEFAULT_TAB_FOLDER_KEY
