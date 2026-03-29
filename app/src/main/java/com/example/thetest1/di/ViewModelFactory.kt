package com.example.thetest1.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.thetest1.data.settings.AppSettingsRepository
import com.example.thetest1.domain.repository.SessionRepository
import com.example.thetest1.domain.repository.SyncRepository
import com.example.thetest1.domain.usecase.AddAudioNoteUseCase
import com.example.thetest1.domain.usecase.AddGoalUseCase
import com.example.thetest1.domain.usecase.AddSessionUseCase
import com.example.thetest1.domain.usecase.AddTextNoteUseCase
import com.example.thetest1.domain.usecase.AddUserTabUseCase
import com.example.thetest1.domain.usecase.AskAiAssistantUseCase
import com.example.thetest1.domain.usecase.DeleteAudioNoteUseCase
import com.example.thetest1.domain.usecase.DeleteGoalUseCase
import com.example.thetest1.domain.usecase.DeleteTextNoteUseCase
import com.example.thetest1.domain.usecase.DeleteUserTabUseCase
import com.example.thetest1.domain.usecase.GetAllSessionsUseCase
import com.example.thetest1.domain.usecase.GetAudioNotesUseCase
import com.example.thetest1.domain.usecase.GetCompletedLessonsCountUseCase
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
import com.example.thetest1.domain.usecase.ObserveGoalsProgressUseCase
import com.example.thetest1.domain.usecase.ObserveTabsUseCase
import com.example.thetest1.domain.usecase.ObserveTabPlaybackProgressUseCase
import com.example.thetest1.domain.usecase.ObserveUserTabsUseCase
import com.example.thetest1.domain.usecase.RenameUserTabUseCase
import com.example.thetest1.domain.usecase.UpdateGoalUseCase
import com.example.thetest1.domain.usecase.UpdateTabFolderUseCase
import com.example.thetest1.domain.usecase.UpdateTabTagsUseCase
import com.example.thetest1.domain.usecase.UpdateTabUseCase
import com.example.thetest1.domain.usecase.UpdateTextNoteUseCase
import com.example.thetest1.domain.usecase.UpdateTabPlaybackProgressUseCase
import com.example.thetest1.presentation.ai_assistant.AiAssistantViewModel
import com.example.thetest1.presentation.auth.AuthViewModel
import com.example.thetest1.presentation.goals.GoalsViewModel
import com.example.thetest1.presentation.main.MainViewModel
import com.example.thetest1.presentation.main.PracticeHeatmapViewModel
import com.example.thetest1.presentation.main.ThemeViewModel
import com.example.thetest1.presentation.settings.SettingsViewModel
import com.example.thetest1.presentation.tab_list.TabListViewModel
import com.example.thetest1.presentation.tab_viewer.TabViewerViewModel

@Suppress("UNCHECKED_CAST")
class ViewModelFactory(
    private val dependencies: Dependencies
) : ViewModelProvider.Factory {

    data class Dependencies(
        val context: Context,
        val dataStore: DataStore<Preferences>,
        val appSettingsRepository: AppSettingsRepository,
        val getTabsUseCase: GetTabsUseCase,
        val observeTabsUseCase: ObserveTabsUseCase,
        val getLessonUseCase: GetLessonUseCase,
        val getTabItemUseCase: GetTabItemUseCase,
        val getTabFileBytesUseCase: GetTabFileBytesUseCase,
        val getSoundFontBytesUseCase: GetSoundFontBytesUseCase,
        val updateTabUseCase: UpdateTabUseCase,
        val getAudioNotesUseCase: GetAudioNotesUseCase,
        val addAudioNoteUseCase: AddAudioNoteUseCase,
        val deleteAudioNoteUseCase: DeleteAudioNoteUseCase,
        val getTextNotesUseCase: GetTextNotesUseCase,
        val addTextNoteUseCase: AddTextNoteUseCase,
        val updateTextNoteUseCase: UpdateTextNoteUseCase,
        val deleteTextNoteUseCase: DeleteTextNoteUseCase,
        val getAllSessionsUseCase: GetAllSessionsUseCase,
        val addSessionUseCase: AddSessionUseCase,
        val getCompletedLessonsCountUseCase: GetCompletedLessonsCountUseCase,
        val getTotalLessonsCountUseCase: GetTotalLessonsCountUseCase,
        val getUserTabsUseCase: GetUserTabsUseCase,
        val addUserTabUseCase: AddUserTabUseCase,
        val markTabOpenedUseCase: MarkTabOpenedUseCase,
        val updateTabFolderUseCase: UpdateTabFolderUseCase,
        val updateTabTagsUseCase: UpdateTabTagsUseCase,
        val markTabOfflineReadyUseCase: MarkTabOfflineReadyUseCase,
        val getSessionsForLastMonthUseCase: GetSessionsForLastMonthUseCase,
        val deleteUserTabUseCase: DeleteUserTabUseCase,
        val renameUserTabUseCase: RenameUserTabUseCase,
        val getUserTabsCountUseCase: GetUserTabsCountUseCase,
        val observeUserTabsUseCase: ObserveUserTabsUseCase,
        val addGoalUseCase: AddGoalUseCase,
        val updateGoalUseCase: UpdateGoalUseCase,
        val deleteGoalUseCase: DeleteGoalUseCase,
        val askAiAssistantUseCaseProvider: () -> AskAiAssistantUseCase,
        val observeGoalsProgressUseCase: ObserveGoalsProgressUseCase,
        val observeTabPlaybackProgressUseCase: ObserveTabPlaybackProgressUseCase,
        val getTabPlaybackProgressUseCase: GetTabPlaybackProgressUseCase,
        val updateTabPlaybackProgressUseCase: UpdateTabPlaybackProgressUseCase,
        val sessionRepository: SessionRepository,
        val syncRepository: SyncRepository
    )

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(MainViewModel::class.java) -> {
                MainViewModel(
                    dependencies.getAllSessionsUseCase,
                    dependencies.addSessionUseCase,
                    dependencies.getCompletedLessonsCountUseCase,
                    dependencies.getTotalLessonsCountUseCase,
                    dependencies.getUserTabsCountUseCase,
                    dependencies.observeTabPlaybackProgressUseCase
                ) as T
            }

            modelClass.isAssignableFrom(TabListViewModel::class.java) -> {
                TabListViewModel(
                    getTabsUseCase = dependencies.getTabsUseCase,
                    observeTabsUseCase = dependencies.observeTabsUseCase,
                    updateTabUseCase = dependencies.updateTabUseCase,
                    getUserTabsUseCase = dependencies.getUserTabsUseCase,
                    observeUserTabsUseCase = dependencies.observeUserTabsUseCase,
                    addUserTabUseCase = dependencies.addUserTabUseCase,
                    markTabOpenedUseCase = dependencies.markTabOpenedUseCase,
                    updateTabFolderUseCase = dependencies.updateTabFolderUseCase,
                    updateTabTagsUseCase = dependencies.updateTabTagsUseCase,
                    markTabOfflineReadyUseCase = dependencies.markTabOfflineReadyUseCase,
                    deleteUserTabUseCase = dependencies.deleteUserTabUseCase,
                    renameUserTabUseCase = dependencies.renameUserTabUseCase,
                    getAllSessionsUseCase = dependencies.getAllSessionsUseCase,
                    observeTabPlaybackProgressUseCase = dependencies.observeTabPlaybackProgressUseCase,
                    getTabFileBytesUseCase = dependencies.getTabFileBytesUseCase,
                    getSoundFontBytesUseCase = dependencies.getSoundFontBytesUseCase
                ) as T
            }

            modelClass.isAssignableFrom(TabViewerViewModel::class.java) -> {
                TabViewerViewModel(
                    context = dependencies.context,
                    getLessonUseCase = dependencies.getLessonUseCase,
                    getTabItemUseCase = dependencies.getTabItemUseCase,
                    getTabFileBytesUseCase = dependencies.getTabFileBytesUseCase,
                    getSoundFontBytesUseCase = dependencies.getSoundFontBytesUseCase,
                    getAudioNotesUseCase = dependencies.getAudioNotesUseCase,
                    addAudioNoteUseCase = dependencies.addAudioNoteUseCase,
                    deleteAudioNoteUseCase = dependencies.deleteAudioNoteUseCase,
                    getTextNotesUseCase = dependencies.getTextNotesUseCase,
                    addTextNoteUseCase = dependencies.addTextNoteUseCase,
                    updateTextNoteUseCase = dependencies.updateTextNoteUseCase,
                    deleteTextNoteUseCase = dependencies.deleteTextNoteUseCase,
                    getTabPlaybackProgressUseCase = dependencies.getTabPlaybackProgressUseCase,
                    updateTabPlaybackProgressUseCase = dependencies.updateTabPlaybackProgressUseCase
                ) as T
            }

            modelClass.isAssignableFrom(AiAssistantViewModel::class.java) -> {
                AiAssistantViewModel(dependencies.askAiAssistantUseCaseProvider()) as T
            }

            modelClass.isAssignableFrom(PracticeHeatmapViewModel::class.java) -> {
                PracticeHeatmapViewModel(dependencies.getSessionsForLastMonthUseCase) as T
            }

            modelClass.isAssignableFrom(ThemeViewModel::class.java) -> {
                ThemeViewModel(dependencies.appSettingsRepository) as T
            }

            modelClass.isAssignableFrom(GoalsViewModel::class.java) -> {
                GoalsViewModel(
                    addGoalUseCase = dependencies.addGoalUseCase,
                    updateGoalUseCase = dependencies.updateGoalUseCase,
                    deleteGoalUseCase = dependencies.deleteGoalUseCase,
                    observeGoalsProgressUseCase = dependencies.observeGoalsProgressUseCase
                ) as T
            }

            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                AuthViewModel() as T
            }

            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(
                    sessionRepository = dependencies.sessionRepository,
                    syncRepository = dependencies.syncRepository
                ) as T
            }

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
