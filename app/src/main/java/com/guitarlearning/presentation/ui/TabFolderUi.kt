package com.guitarlearning.presentation.ui

import android.content.Context
import com.guitarlearning.R
import com.guitarlearning.domain.model.DEFAULT_TAB_FOLDER_KEY
import com.guitarlearning.domain.model.normalizeTabFolder

fun displayTabFolder(context: Context, folder: String?): String {
    val normalized = normalizeTabFolder(folder)
    return if (normalized == DEFAULT_TAB_FOLDER_KEY) {
        context.getString(R.string.tab_folder_default)
    } else {
        normalized
    }
}
