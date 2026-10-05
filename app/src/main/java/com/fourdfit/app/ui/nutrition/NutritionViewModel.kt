package com.fourdfit.app.ui.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourdfit.app.domain.model.MealKey
import com.fourdfit.app.domain.model.MealType
import com.fourdfit.app.domain.model.NutritionDay
import com.fourdfit.app.domain.repository.NutritionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NutritionUiState(
    val loading: Boolean = true,
    val days: List<NutritionDay> = emptyList(),
    val currentDay: Int = 1,
    val selectedDay: Int = 1,
    val completions: Set<MealKey> = emptySet(),
) {
    val selected: NutritionDay? get() = days.firstOrNull { it.day == selectedDay }
    val totalMeals: Int get() = days.size * MealType.entries.size

    fun doneOn(day: Int): Int = completions.count { it.day == day }

    fun isDone(
        day: Int,
        type: MealType,
    ): Boolean = MealKey(day, type) in completions
}

class NutritionViewModel(
    private val repository: NutritionRepository,
) : ViewModel() {
    private val selected = MutableStateFlow<Int?>(null)

    val state: StateFlow<NutritionUiState> =
        combine(
            repository.observePlan(),
            repository.observeCompletions(),
            repository.observeCurrentDay(),
            selected,
        ) { plan, done, current, chosen ->
            NutritionUiState(false, plan, current, chosen ?: current, done)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NutritionUiState())

    init {
        viewModelScope.launch { repository.refresh() }
    }

    fun selectDay(day: Int) {
        selected.value = day
    }

    fun setMeal(
        day: Int,
        type: MealType,
        done: Boolean,
    ) {
        viewModelScope.launch { repository.setMealCompleted(day, type, done) }
    }
}
