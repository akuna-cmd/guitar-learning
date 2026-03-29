package com.example.thetest1.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.example.thetest1.BuildConfig
import com.example.thetest1.data.local.AppDatabase
import com.example.thetest1.data.local.Migration12To13
import com.example.thetest1.data.remote.AiAssistantConfig
import com.example.thetest1.data.repository.FirestoreSyncRepositoryImpl
import com.example.thetest1.data.repository.AudioNoteRepositoryImpl
import com.example.thetest1.data.repository.AiAssistantRepositoryImpl
import com.example.thetest1.data.repository.GoalRepositoryImpl
import com.example.thetest1.data.repository.SessionRepositoryImpl
import com.example.thetest1.data.repository.SoundFontRepositoryImpl
import com.example.thetest1.data.repository.TabFileRepositoryImpl
import com.example.thetest1.data.repository.TabPlaybackProgressRepositoryImpl
import com.example.thetest1.data.repository.TabRepositoryImpl
import com.example.thetest1.data.repository.TextNoteRepositoryImpl
import com.example.thetest1.data.settings.AppSettingsRepository
import com.example.thetest1.domain.repository.AiAssistantRepository
import com.example.thetest1.domain.repository.AudioNoteRepository
import com.example.thetest1.domain.repository.GoalRepository
import com.example.thetest1.domain.repository.SessionRepository
import com.example.thetest1.domain.repository.SoundFontRepository
import com.example.thetest1.domain.repository.TabFileRepository
import com.example.thetest1.domain.repository.TabPlaybackProgressRepository
import com.example.thetest1.domain.repository.TabRepository
import com.example.thetest1.domain.repository.TextNoteRepository
import com.example.thetest1.domain.repository.SyncRepository
import com.example.thetest1.domain.usecase.AskAiAssistantUseCase
import com.example.thetest1.domain.usecase.AddAudioNoteUseCase
import com.example.thetest1.domain.usecase.AddGoalUseCase
import com.example.thetest1.domain.usecase.AddSessionUseCase
import com.example.thetest1.domain.usecase.AddTextNoteUseCase
import com.example.thetest1.domain.usecase.AddUserTabUseCase
import com.example.thetest1.domain.usecase.DeleteAudioNoteUseCase
import com.example.thetest1.domain.usecase.DeleteGoalUseCase
import com.example.thetest1.domain.usecase.DeleteTextNoteUseCase
import com.example.thetest1.domain.usecase.DeleteUserTabUseCase
import com.example.thetest1.domain.usecase.GetAllSessionsUseCase
import com.example.thetest1.domain.usecase.GetAudioNotesUseCase
import com.example.thetest1.domain.usecase.GetCompletedLessonsCountUseCase
import com.example.thetest1.domain.usecase.GetGoalsUseCase
import com.example.thetest1.domain.usecase.GetLessonUseCase
import com.example.thetest1.domain.usecase.GetSessionsForLastMonthUseCase
import com.example.thetest1.domain.usecase.GetSoundFontBytesUseCase
import com.example.thetest1.domain.usecase.GetTabFileBytesUseCase
import com.example.thetest1.domain.usecase.GetTabItemUseCase
import com.example.thetest1.domain.usecase.GetTabsUseCase
import com.example.thetest1.domain.usecase.GetTextNotesUseCase
import com.example.thetest1.domain.usecase.GetTotalLessonsCountUseCase
import com.example.thetest1.domain.usecase.GetTabPlaybackProgressUseCase
import com.example.thetest1.domain.usecase.GetUserTabsCountUseCase
import com.example.thetest1.domain.usecase.GetUserTabsUseCase
import com.example.thetest1.domain.usecase.MarkTabOfflineReadyUseCase
import com.example.thetest1.domain.usecase.MarkTabOpenedUseCase
import com.example.thetest1.domain.usecase.ObserveTabPlaybackProgressUseCase
import com.example.thetest1.domain.usecase.ObserveGoalsProgressUseCase
import com.example.thetest1.domain.usecase.ObserveTabsUseCase
import com.example.thetest1.domain.usecase.ObserveUserTabsUseCase
import com.example.thetest1.domain.usecase.RenameUserTabUseCase
import com.example.thetest1.domain.usecase.UpdateGoalUseCase
import com.example.thetest1.domain.usecase.UpdateTabUseCase
import com.example.thetest1.domain.usecase.UpdateTabFolderUseCase
import com.example.thetest1.domain.usecase.UpdateTabTagsUseCase
import com.example.thetest1.domain.usecase.UpdateTextNoteUseCase
import com.example.thetest1.domain.usecase.UpdateTabPlaybackProgressUseCase
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AppContainer(val context: Context) {

    private val appDatabase: AppDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java, "app_database"
        ).addMigrations(Migration12To13)
            .fallbackToDestructiveMigration()
            .build()
    }

    private val audioNoteDao by lazy { appDatabase.audioNoteDao() }
    private val textNoteDao by lazy { appDatabase.textNoteDao() }
    private val sessionDao by lazy { appDatabase.sessionDao() }
    private val tabDao by lazy { appDatabase.tabDao() }
    private val goalDao by lazy { appDatabase.goalDao() }

    private val tabRepository: TabRepository by lazy { TabRepositoryImpl(context, tabDao) }
    private val tabFileRepository: TabFileRepository by lazy { TabFileRepositoryImpl(context) }
    private val soundFontRepository: SoundFontRepository by lazy { SoundFontRepositoryImpl(context) }
    private val aiAssistantRepository: AiAssistantRepository by lazy {
        AiAssistantRepositoryImpl(
            GenerativeModel(
                modelName = AiAssistantConfig.ModelName,
                apiKey = BuildConfig.GEMINI_API_KEY
            )
        )
    }
    private val audioNoteRepository: AudioNoteRepository by lazy {
        AudioNoteRepositoryImpl(
            audioNoteDao
        )
    }
    private val textNoteRepository: TextNoteRepository by lazy { TextNoteRepositoryImpl(textNoteDao) }
    private val sessionRepository: SessionRepository by lazy { SessionRepositoryImpl(sessionDao) }
    private val goalRepository: GoalRepository by lazy { GoalRepositoryImpl(goalDao) }
    private val tabPlaybackProgressRepository: TabPlaybackProgressRepository by lazy {
        TabPlaybackProgressRepositoryImpl(context.dataStore)
    }
    private val appSettingsRepository by lazy { AppSettingsRepository(context.dataStore) }
    private val syncRepository: SyncRepository by lazy {
        FirestoreSyncRepositoryImpl(
            context = context,
            tabRepository = tabRepository,
            sessionRepository = sessionRepository,
            goalRepository = goalRepository,
            progressRepository = tabPlaybackProgressRepository,
            appSettingsRepository = appSettingsRepository
        )
    }

    private val getTabsUseCase by lazy { GetTabsUseCase(tabRepository) }
    private val observeTabsUseCase by lazy { ObserveTabsUseCase(tabRepository) }
    private val getLessonUseCase by lazy { GetLessonUseCase(tabRepository) }
    private val getTabItemUseCase by lazy { GetTabItemUseCase(tabRepository) }
    private val updateTabUseCase by lazy { UpdateTabUseCase(tabRepository) }
    private val getTabFileBytesUseCase by lazy { GetTabFileBytesUseCase(tabFileRepository) }
    private val getSoundFontBytesUseCase by lazy { GetSoundFontBytesUseCase(soundFontRepository) }
    private val getCompletedLessonsCountUseCase by lazy {
        GetCompletedLessonsCountUseCase(
            tabRepository
        )
    }
    private val getTotalLessonsCountUseCase by lazy { GetTotalLessonsCountUseCase(tabRepository) }
    private val getUserTabsUseCase by lazy { GetUserTabsUseCase(tabRepository) }
    private val observeUserTabsUseCase by lazy { ObserveUserTabsUseCase(tabRepository) }
    private val addUserTabUseCase by lazy { AddUserTabUseCase(tabRepository) }
    private val markTabOpenedUseCase by lazy { MarkTabOpenedUseCase(tabRepository) }
    private val updateTabFolderUseCase by lazy { UpdateTabFolderUseCase(tabRepository) }
    private val updateTabTagsUseCase by lazy { UpdateTabTagsUseCase(tabRepository) }
    private val markTabOfflineReadyUseCase by lazy { MarkTabOfflineReadyUseCase(tabRepository) }
    private val deleteUserTabUseCase by lazy { DeleteUserTabUseCase(tabRepository) }
    private val renameUserTabUseCase by lazy { RenameUserTabUseCase(tabRepository) }
    private val getUserTabsCountUseCase by lazy { GetUserTabsCountUseCase(tabRepository) }
    private val askAiAssistantUseCase by lazy { AskAiAssistantUseCase(aiAssistantRepository) }

    private val getAudioNotesUseCase by lazy { GetAudioNotesUseCase(audioNoteRepository) }
    private val addAudioNoteUseCase by lazy { AddAudioNoteUseCase(audioNoteRepository) }
    private val deleteAudioNoteUseCase by lazy { DeleteAudioNoteUseCase(audioNoteRepository) }

    private val getTextNotesUseCase by lazy { GetTextNotesUseCase(textNoteRepository) }
    private val addTextNoteUseCase by lazy { AddTextNoteUseCase(textNoteRepository) }
    private val updateTextNoteUseCase by lazy { UpdateTextNoteUseCase(textNoteRepository) }
    private val deleteTextNoteUseCase by lazy { DeleteTextNoteUseCase(textNoteRepository) }

    private val getAllSessionsUseCase by lazy { GetAllSessionsUseCase(sessionRepository) }
    private val addSessionUseCase by lazy { AddSessionUseCase(sessionRepository) }
    private val getSessionsForLastMonthUseCase by lazy {
        GetSessionsForLastMonthUseCase(
            sessionRepository
        )
    }
    private val observeTabPlaybackProgressUseCase by lazy {
        ObserveTabPlaybackProgressUseCase(tabPlaybackProgressRepository)
    }
    private val getTabPlaybackProgressUseCase by lazy {
        GetTabPlaybackProgressUseCase(tabPlaybackProgressRepository)
    }
    private val updateTabPlaybackProgressUseCase by lazy {
        UpdateTabPlaybackProgressUseCase(tabPlaybackProgressRepository)
    }

    private val getGoalsUseCase by lazy { GetGoalsUseCase(goalRepository) }
    private val addGoalUseCase by lazy { AddGoalUseCase(goalRepository) }
    private val updateGoalUseCase by lazy { UpdateGoalUseCase(goalRepository) }
    private val deleteGoalUseCase by lazy { DeleteGoalUseCase(goalRepository) }
    private val observeGoalsProgressUseCase by lazy {
        ObserveGoalsProgressUseCase(
            getGoalsUseCase = getGoalsUseCase,
            getCompletedLessonsCountUseCase = getCompletedLessonsCountUseCase,
            getAllSessionsUseCase = getAllSessionsUseCase
        )
    }

    val viewModelFactory: ViewModelFactory by lazy {
        ViewModelFactory(
            dependencies = ViewModelFactory.Dependencies(
                context = context,
                dataStore = context.dataStore,
                appSettingsRepository = appSettingsRepository,
                getTabsUseCase = getTabsUseCase,
                observeTabsUseCase = observeTabsUseCase,
                getLessonUseCase = getLessonUseCase,
                getTabItemUseCase = getTabItemUseCase,
                getTabFileBytesUseCase = getTabFileBytesUseCase,
                getSoundFontBytesUseCase = getSoundFontBytesUseCase,
                updateTabUseCase = updateTabUseCase,
                getAudioNotesUseCase = getAudioNotesUseCase,
                addAudioNoteUseCase = addAudioNoteUseCase,
                deleteAudioNoteUseCase = deleteAudioNoteUseCase,
                getTextNotesUseCase = getTextNotesUseCase,
                addTextNoteUseCase = addTextNoteUseCase,
                updateTextNoteUseCase = updateTextNoteUseCase,
                deleteTextNoteUseCase = deleteTextNoteUseCase,
                getAllSessionsUseCase = getAllSessionsUseCase,
                addSessionUseCase = addSessionUseCase,
                getCompletedLessonsCountUseCase = getCompletedLessonsCountUseCase,
                getTotalLessonsCountUseCase = getTotalLessonsCountUseCase,
                getUserTabsUseCase = getUserTabsUseCase,
                observeUserTabsUseCase = observeUserTabsUseCase,
                addUserTabUseCase = addUserTabUseCase,
                markTabOpenedUseCase = markTabOpenedUseCase,
                updateTabFolderUseCase = updateTabFolderUseCase,
                updateTabTagsUseCase = updateTabTagsUseCase,
                markTabOfflineReadyUseCase = markTabOfflineReadyUseCase,
                getSessionsForLastMonthUseCase = getSessionsForLastMonthUseCase,
                deleteUserTabUseCase = deleteUserTabUseCase,
                renameUserTabUseCase = renameUserTabUseCase,
                getUserTabsCountUseCase = getUserTabsCountUseCase,
                addGoalUseCase = addGoalUseCase,
                updateGoalUseCase = updateGoalUseCase,
                deleteGoalUseCase = deleteGoalUseCase,
                askAiAssistantUseCaseProvider = { askAiAssistantUseCase },
                observeGoalsProgressUseCase = observeGoalsProgressUseCase,
                observeTabPlaybackProgressUseCase = observeTabPlaybackProgressUseCase,
                getTabPlaybackProgressUseCase = getTabPlaybackProgressUseCase,
                updateTabPlaybackProgressUseCase = updateTabPlaybackProgressUseCase,
                sessionRepository = sessionRepository,
                syncRepository = syncRepository
            )
        )
    }

    suspend fun warmUp() {
        runCatching { tabRepository.getTabs().first() }
        runCatching { tabRepository.getUserTabs() }
        runCatching { tabPlaybackProgressRepository.observeAll().first() }
        runCatching { FirebaseAuth.getInstance().currentUser }
        runCatching { context.assets.open("tab_viewer.html").close() }
        runCatching { context.assets.open("alphatab_local.js").close() }
    }
}
