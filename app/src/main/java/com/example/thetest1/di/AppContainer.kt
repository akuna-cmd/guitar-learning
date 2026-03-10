package com.example.thetest1.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.example.thetest1.data.local.AppDatabase
import com.example.thetest1.data.repository.AudioNoteRepositoryImpl
import com.example.thetest1.data.repository.GoalRepositoryImpl
import com.example.thetest1.data.repository.SessionRepositoryImpl
import com.example.thetest1.data.repository.TabRepositoryImpl
import com.example.thetest1.data.repository.TextNoteRepositoryImpl
import com.example.thetest1.domain.repository.AudioNoteRepository
import com.example.thetest1.domain.repository.GoalRepository
import com.example.thetest1.domain.repository.SessionRepository
import com.example.thetest1.domain.repository.TabRepository
import com.example.thetest1.domain.repository.TextNoteRepository
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
import com.example.thetest1.domain.usecase.GetTabItemUseCase
import com.example.thetest1.domain.usecase.GetTabsUseCase
import com.example.thetest1.domain.usecase.GetTextNotesUseCase
import com.example.thetest1.domain.usecase.GetTotalLessonsCountUseCase
import com.example.thetest1.domain.usecase.GetUserTabsCountUseCase
import com.example.thetest1.domain.usecase.GetUserTabsUseCase
import com.example.thetest1.domain.usecase.RenameUserTabUseCase
import com.example.thetest1.domain.usecase.UpdateGoalUseCase
import com.example.thetest1.domain.usecase.UpdateTabUseCase
import com.example.thetest1.domain.usecase.UpdateTextNoteUseCase

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AppContainer(val context: Context) {

    private val appDatabase: AppDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java, "app_database"
        ).fallbackToDestructiveMigration().build()
    }

    private val audioNoteDao by lazy { appDatabase.audioNoteDao() }
    private val textNoteDao by lazy { appDatabase.textNoteDao() }
    private val sessionDao by lazy { appDatabase.sessionDao() }
    private val tabDao by lazy { appDatabase.tabDao() }
    private val goalDao by lazy { appDatabase.goalDao() }

    private val tabRepository: TabRepository by lazy { TabRepositoryImpl(context, tabDao) }
    private val audioNoteRepository: AudioNoteRepository by lazy {
        AudioNoteRepositoryImpl(
            audioNoteDao
        )
    }
    private val textNoteRepository: TextNoteRepository by lazy { TextNoteRepositoryImpl(textNoteDao) }
    private val sessionRepository: SessionRepository by lazy { SessionRepositoryImpl(sessionDao) }
    private val goalRepository: GoalRepository by lazy { GoalRepositoryImpl(goalDao) }

    private val getTabsUseCase by lazy { GetTabsUseCase(tabRepository) }
    private val getLessonUseCase by lazy { GetLessonUseCase(tabRepository) }
    private val getTabItemUseCase by lazy { GetTabItemUseCase(tabRepository) }
    private val updateTabUseCase by lazy { UpdateTabUseCase(tabRepository) }
    private val getCompletedLessonsCountUseCase by lazy {
        GetCompletedLessonsCountUseCase(
            tabRepository
        )
    }
    private val getTotalLessonsCountUseCase by lazy { GetTotalLessonsCountUseCase(tabRepository) }
    private val getUserTabsUseCase by lazy { GetUserTabsUseCase(tabRepository) }
    private val addUserTabUseCase by lazy { AddUserTabUseCase(tabRepository) }
    private val deleteUserTabUseCase by lazy { DeleteUserTabUseCase(tabRepository) }
    private val renameUserTabUseCase by lazy { RenameUserTabUseCase(tabRepository) }
    private val getUserTabsCountUseCase by lazy { GetUserTabsCountUseCase(tabRepository) }

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

    private val getGoalsUseCase by lazy { GetGoalsUseCase(goalRepository) }
    private val addGoalUseCase by lazy { AddGoalUseCase(goalRepository) }
    private val updateGoalUseCase by lazy { UpdateGoalUseCase(goalRepository) }
    private val deleteGoalUseCase by lazy { DeleteGoalUseCase(goalRepository) }

    val viewModelFactory: ViewModelFactory by lazy {
        ViewModelFactory(
            context = context,
            dataStore = context.dataStore,
            getTabsUseCase = getTabsUseCase,
            getLessonUseCase = getLessonUseCase,
            getTabItemUseCase = getTabItemUseCase,
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
            addUserTabUseCase = addUserTabUseCase,
            getSessionsForLastMonthUseCase = getSessionsForLastMonthUseCase,
            deleteUserTabUseCase = deleteUserTabUseCase,
            renameUserTabUseCase = renameUserTabUseCase,
            getUserTabsCountUseCase = getUserTabsCountUseCase,
            getGoalsUseCase = getGoalsUseCase,
            addGoalUseCase = addGoalUseCase,
            updateGoalUseCase = updateGoalUseCase,
            deleteGoalUseCase = deleteGoalUseCase
        )
    }
}