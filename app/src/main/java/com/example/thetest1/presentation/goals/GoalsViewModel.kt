package com.example.thetest1.presentation.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thetest1.domain.model.Goal
import com.example.thetest1.domain.usecase.AddGoalUseCase
import com.example.thetest1.domain.usecase.DeleteGoalUseCase
import com.example.thetest1.domain.usecase.ObserveGoalsProgressUseCase
import com.example.thetest1.domain.usecase.UpdateGoalUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class GoalsUiState(
    val goals: List<Goal> = emptyList(),
    val showAddGoalDialog: Boolean = false,
    val goalToEdit: Goal? = null
)

class GoalsViewModel(
    private val addGoalUseCase: AddGoalUseCase,
    private val updateGoalUseCase: UpdateGoalUseCase,
    private val deleteGoalUseCase: DeleteGoalUseCase,
    private val observeGoalsProgressUseCase: ObserveGoalsProgressUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalsUiState())
    val uiState: StateFlow<GoalsUiState> = _uiState.asStateFlow()

    init {
        observeGoalsProgressUseCase()
            .onEach { goals ->
                _uiState.value = _uiState.value.copy(goals = goals)
            }
            .launchIn(viewModelScope)
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
