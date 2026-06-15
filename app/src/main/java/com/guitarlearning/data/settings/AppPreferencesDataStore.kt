package com.guitarlearning.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile

object AppPreferencesDataStore {
    private const val SETTINGS_FILE_NAME = "settings"

    @Volatile
    private var instance: DataStore<Preferences>? = null

    fun getInstance(context: Context): DataStore<Preferences> {
        return instance ?: synchronized(this) {
            instance ?: PreferenceDataStoreFactory.create {
                context.applicationContext.preferencesDataStoreFile(SETTINGS_FILE_NAME)
            }.also { instance = it }
        }
    }
}
