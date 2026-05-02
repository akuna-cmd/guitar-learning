package com.guitarlearning.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import com.guitarlearning.data.local.AppDatabase
import com.guitarlearning.data.local.AudioNoteDao
import com.guitarlearning.data.local.GoalDao
import com.guitarlearning.data.local.Migration12To13
import com.guitarlearning.data.local.Migration13To14
import com.guitarlearning.data.local.Migration14To15
import com.guitarlearning.data.local.SessionDao
import com.guitarlearning.data.local.TabDao
import com.guitarlearning.data.local.TextNoteDao
import com.guitarlearning.data.remote.AiAssistantConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("settings")
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "app_database"
        ).addMigrations(Migration12To13, Migration13To14, Migration14To15)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideAudioNoteDao(database: AppDatabase): AudioNoteDao = database.audioNoteDao()

    @Provides
    fun provideTextNoteDao(database: AppDatabase): TextNoteDao = database.textNoteDao()

    @Provides
    fun provideSessionDao(database: AppDatabase): SessionDao = database.sessionDao()

    @Provides
    fun provideTabDao(database: AppDatabase): TabDao = database.tabDao()

    @Provides
    fun provideGoalDao(database: AppDatabase): GoalDao = database.goalDao()

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseRemoteConfig(): FirebaseRemoteConfig {
        return FirebaseRemoteConfig.getInstance().apply {
            setConfigSettingsAsync(
                remoteConfigSettings {
                    minimumFetchIntervalInSeconds = 21_600L
                }
            )
            setDefaultsAsync(
                mapOf(AiAssistantConfig.RemoteModelKey to AiAssistantConfig.DefaultModelName)
            )
        }
    }
}
