package com.guitarlearning.presentation.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guitarlearning.domain.repository.GoalRepository
import com.guitarlearning.domain.model.Goal
import com.guitarlearning.domain.usecase.ObserveGoalsProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GoalsUiState(
    val goals: List<Goal> = emptyList(),
    val showAddGoalDialog: Boolean = false,
    val goalToEdit: Goal? = null
)

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
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
            goalRepository.addGoal(goal)
        }
    }

    fun updateGoal(goal: Goal) {
        viewModelScope.launch {
            goalRepository.updateGoal(goal)
        }
    }

    fun deleteGoal(goal: Goal) {
        viewModelScope.launch {
            goalRepository.deleteGoal(goal)
        }
    }

    fun toggleCustomGoal(goal: Goal) {
        viewModelScope.launch {
            goalRepository.updateGoal(goal.copy(isCompleted = !goal.isCompleted))
        }
    }
}
