package com.example.thetest1.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
import com.example.thetest1.presentation.ai_assistant.AiAssistantViewModel
import com.example.thetest1.presentation.goals.GoalsViewModel
import com.example.thetest1.presentation.main.MainViewModel
import com.example.thetest1.presentation.main.PracticeHeatmapViewModel
import com.example.thetest1.presentation.main.ThemeViewModel
import com.example.thetest1.presentation.tab_list.TabListViewModel
import com.example.thetest1.presentation.tab_viewer.TabViewerViewModel

@Suppress("UNCHECKED_CAST")
class ViewModelFactory(
    private val context: Context,
    private val dataStore: DataStore<Preferences>,
    private val getTabsUseCase: GetTabsUseCase,
    private val getLessonUseCase: GetLessonUseCase,
    private val getTabItemUseCase: GetTabItemUseCase,
    private val updateTabUseCase: UpdateTabUseCase,
    private val getAudioNotesUseCase: GetAudioNotesUseCase,
    private val addAudioNoteUseCase: AddAudioNoteUseCase,
    private val deleteAudioNoteUseCase: DeleteAudioNoteUseCase,
    private val getTextNotesUseCase: GetTextNotesUseCase,
    private val addTextNoteUseCase: AddTextNoteUseCase,
    private val updateTextNoteUseCase: UpdateTextNoteUseCase,
    private val deleteTextNoteUseCase: DeleteTextNoteUseCase,
    private val getAllSessionsUseCase: GetAllSessionsUseCase,
    private val addSessionUseCase: AddSessionUseCase,
    private val getCompletedLessonsCountUseCase: GetCompletedLessonsCountUseCase,
    private val getTotalLessonsCountUseCase: GetTotalLessonsCountUseCase,
    private val getUserTabsUseCase: GetUserTabsUseCase,
    private val addUserTabUseCase: AddUserTabUseCase,
    private val getSessionsForLastMonthUseCase: GetSessionsForLastMonthUseCase,
    private val deleteUserTabUseCase: DeleteUserTabUseCase,
    private val renameUserTabUseCase: RenameUserTabUseCase,
    private val getUserTabsCountUseCase: GetUserTabsCountUseCase,
    private val getGoalsUseCase: GetGoalsUseCase,
    private val addGoalUseCase: AddGoalUseCase,
    private val updateGoalUseCase: UpdateGoalUseCase,
    private val deleteGoalUseCase: DeleteGoalUseCase
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(MainViewModel::class.java) -> {
                MainViewModel(
                    getAllSessionsUseCase,
                    addSessionUseCase,
                    getCompletedLessonsCountUseCase,
                    getTotalLessonsCountUseCase,
                    getUserTabsCountUseCase
                ) as T
            }

            modelClass.isAssignableFrom(TabListViewModel::class.java) -> {
                TabListViewModel(
                    getTabsUseCase = getTabsUseCase,
                    updateTabUseCase = updateTabUseCase,
                    getUserTabsUseCase = getUserTabsUseCase,
                    addUserTabUseCase = addUserTabUseCase,
                    deleteUserTabUseCase = deleteUserTabUseCase,
                    renameUserTabUseCase = renameUserTabUseCase
                ) as T
            }

            modelClass.isAssignableFrom(TabViewerViewModel::class.java) -> {
                TabViewerViewModel(
                    context = context,
                    getLessonUseCase = getLessonUseCase,
                    getTabItemUseCase = getTabItemUseCase,
                    getAudioNotesUseCase = getAudioNotesUseCase,
                    addAudioNoteUseCase = addAudioNoteUseCase,
                    deleteAudioNoteUseCase = deleteAudioNoteUseCase,
                    getTextNotesUseCase = getTextNotesUseCase,
                    addTextNoteUseCase = addTextNoteUseCase,
                    updateTextNoteUseCase = updateTextNoteUseCase,
                    deleteTextNoteUseCase = deleteTextNoteUseCase
                ) as T
            }

            modelClass.isAssignableFrom(AiAssistantViewModel::class.java) -> {
                AiAssistantViewModel() as T
            }

            modelClass.isAssignableFrom(PracticeHeatmapViewModel::class.java) -> {
                PracticeHeatmapViewModel(getSessionsForLastMonthUseCase) as T
            }

            modelClass.isAssignableFrom(ThemeViewModel::class.java) -> {
                ThemeViewModel(dataStore) as T
            }

            modelClass.isAssignableFrom(GoalsViewModel::class.java) -> {
                GoalsViewModel(
                    getGoalsUseCase = getGoalsUseCase,
                    addGoalUseCase = addGoalUseCase,
                    updateGoalUseCase = updateGoalUseCase,
                    deleteGoalUseCase = deleteGoalUseCase,
                    getCompletedLessonsCountUseCase = getCompletedLessonsCountUseCase,
                    getAllSessionsUseCase = getAllSessionsUseCase
                ) as T
            }

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
