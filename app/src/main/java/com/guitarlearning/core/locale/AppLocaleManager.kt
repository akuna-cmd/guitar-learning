package com.guitarlearning.core.locale

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import androidx.datastore.preferences.core.stringPreferencesKey
import com.guitarlearning.data.settings.AppPreferencesDataStore
import com.guitarlearning.domain.settings.AppLanguage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale

object AppLocaleManager {
    private val appLanguageKey = stringPreferencesKey("app_language")

    fun wrap(context: Context): ContextWrapper {
        val languageTag = getSavedLanguageTag(context)
        val locale = Locale.forLanguageTag(languageTag)
        Locale.setDefault(locale)

        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)

        return ContextWrapper(context.createConfigurationContext(configuration))
    }

    fun getSavedLanguageTag(context: Context): String {
        val appContext = context.applicationContext
        val appLanguageName = runBlocking {
            AppPreferencesDataStore.getInstance(appContext).data.first()[appLanguageKey]
        }

        return appLanguageName
            ?.let(::appLanguageFromName)
            ?.languageTag
            ?: AppLanguage.UKRAINIAN.languageTag
    }

    private fun appLanguageFromName(name: String): AppLanguage? {
        return try {
            AppLanguage.valueOf(name)
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
