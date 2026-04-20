package com.guitarlearning.core

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import java.util.Locale

object AppLocaleManager {
    private const val PREFS_NAME = "app_locale_prefs"
    private const val KEY_LANGUAGE_TAG = "language_tag"
    private const val DEFAULT_LANGUAGE_TAG = "uk"

    fun wrap(context: Context): ContextWrapper {
        val languageTag = getSavedLanguageTag(context)
        val locale = Locale.forLanguageTag(languageTag)
        Locale.setDefault(locale)

        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)

        return ContextWrapper(context.createConfigurationContext(configuration))
    }

    fun persistLanguage(context: Context, languageTag: String) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE_TAG, languageTag)
            .apply()
    }

    fun getSavedLanguageTag(context: Context): String {
        return context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE_TAG, DEFAULT_LANGUAGE_TAG)
            ?: DEFAULT_LANGUAGE_TAG
    }
}
