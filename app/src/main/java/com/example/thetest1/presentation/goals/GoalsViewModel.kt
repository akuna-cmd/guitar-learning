package com.example.thetest1.presentation.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thetest1.domain.model.Goal
import com.example.thetest1.domain.model.GoalType
import com.example.thetest1.domain.usecase.AddGoalUseCase
import com.example.thetest1.domain.usecase.DeleteGoalUseCase
import com.example.thetest1.domain.usecase.GetAllSessionsUseCase
import com.example.thetest1.domain.usecase.GetCompletedLessonsCountUseCase
import com.example.thetest1.domain.usecase.GetGoalsUseCase
import com.example.thetest1.domain.usecase.UpdateGoalUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class GoalsUiState(
    val goals: List<Goal> = emptyList(),
    val showAddGoalDialog: Boolean = false,
    val goalToEdit: Goal? = null
)

class GoalsViewModel(
    private val getGoalsUseCase: GetGoalsUseCase,
    private val addGoalUseCase: AddGoalUseCase,
    private val updateGoalUseCase: UpdateGoalUseCase,
    private val deleteGoalUseCase: DeleteGoalUseCase,
    private val getCompletedLessonsCountUseCase: GetCompletedLessonsCountUseCase,
    private val getAllSessionsUseCase: GetAllSessionsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalsUiState())
    val uiState: StateFlow<GoalsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                getGoalsUseCase(),
                getCompletedLessonsCountUseCase(),
                getAllSessionsUseCase()
            ) { goals, completedLessons, sessions ->
                val updatedGoals = goals.map { goal ->
                    val progress = when (goal.type) {
                        GoalType.LESSONS_COMPLETED -> completedLessons
                        GoalType.SESSION_TIME -> (sessions.sumOf { it.duration } / 60000).toInt()
                        GoalType.CUSTOM -> if (goal.isCompleted) 1 else 0
                    }
                    val isCompleted =
                        if (goal.type == GoalType.CUSTOM) goal.isCompleted else progress >= goal.target

                    val deadlineCalendar =
                        Calendar.getInstance().apply { timeInMillis = goal.deadline }
                    val todayCalendar = Calendar.getInstance()

                    val isOverdue =
                        if (deadlineCalendar.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR) &&
                            deadlineCalendar.get(Calendar.DAY_OF_YEAR) == todayCalendar.get(Calendar.DAY_OF_YEAR)
                        ) {
                            false
                        } else {
                            System.currentTimeMillis() > goal.deadline && !isCompleted
                        }

                    goal.copy(progress = progress, isCompleted = isCompleted, isOverdue = isOverdue)
                }
                _uiState.value = _uiState.value.copy(goals = updatedGoals)
            }.launchIn(viewModelScope)
        }
    }

    fun onAddGoalClicked() {
        _uiState.value = _uiState.value.copy(showAddGoalDialog = true, goalToEdit = null)
    }

    fun onEditGoalClicked(goal: Goal) {
        _uiState.value = _uiState.value.copy(showAddGoalDialog = true, goalToEdit = goal)
    }

    fun onDismissGoalDialog() {
        _uiState.value = _uiState.value.copy(showAddGoalDialog = false, goalToEdit = null)
    }

    fun addGoal(goal: Goal) {
        viewModelScope.launch {
            addGoalUseCase(goal)
        }
    }

    fun updateGoal(goal: Goal) {
        viewModelScope.launch {
            updateGoalUseCase(goal)
        }
    }

    fun deleteGoal(goal: Goal) {
        viewModelScope.launch {
            deleteGoalUseCase(goal)
        }
    }

    fun toggleCustomGoal(goal: Goal) {
        viewModelScope.launch {
            updateGoalUseCase(goal.copy(isCompleted = !goal.isCompleted))
        }
    }
}
