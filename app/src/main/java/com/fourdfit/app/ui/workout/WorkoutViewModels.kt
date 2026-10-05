package com.fourdfit.app.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourdfit.app.domain.model.Difficulty
import com.fourdfit.app.domain.model.Exercise
import com.fourdfit.app.domain.model.ExerciseCategory
import com.fourdfit.app.domain.model.WorkoutProgram
import com.fourdfit.app.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

const val SAFETY_NOTE =
    "Move within a comfortable range. Stop if you feel pain, dizziness or shortness of breath, and check with a health professional if you have an injury or medical condition."

sealed interface LibraryFilter {
    val label: String

    data object All : LibraryFilter {
        override val label: String = "All"
    }

    data class Category(
        val category: ExerciseCategory,
    ) : LibraryFilter {
        override val label: String get() = category.label
    }

    data class Level(
        val difficulty: Difficulty,
    ) : LibraryFilter {
        override val label: String get() = difficulty.label
    }
}

val LibraryFilters: List<LibraryFilter> =
    listOf<LibraryFilter>(LibraryFilter.All) +
        ExerciseCategory.entries.map { LibraryFilter.Category(it) } +
        Difficulty.entries.map { LibraryFilter.Level(it) }

data class ProgramCardModel(
    val program: WorkoutProgram,
    val preview: Exercise?,
    val exerciseCount: Int,
)

data class LibraryUiState(
    val loading: Boolean = true,
    val totalExercises: Int = 0,
    val programs: List<ProgramCardModel> = emptyList(),
    val exercises: List<Exercise> = emptyList(),
    val filter: LibraryFilter = LibraryFilter.All,
)

class WorkoutLibraryViewModel(
    private val repository: WorkoutRepository,
) : ViewModel() {
    private val filter = MutableStateFlow<LibraryFilter>(LibraryFilter.All)
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val state: StateFlow<LibraryUiState> =
        combine(repository.observeCatalog(), filter, _query) { catalog, f, q ->
            val search = q.trim()

            fun matches(
                categories: List<ExerciseCategory>,
                difficulty: Difficulty,
            ) = when (f) {
                LibraryFilter.All -> true
                is LibraryFilter.Category -> f.category in categories
                is LibraryFilter.Level -> f.difficulty == difficulty
            }
            val exercises =
                catalog.exercises.filter { e ->
                    matches(e.categories, e.difficulty) &&
                        (search.isEmpty() || e.name.contains(search, true) || e.targetMuscles.any { it.contains(search, true) })
                }
            val programs =
                if (search.isNotEmpty()) {
                    emptyList()
                } else {
                    catalog.programs.filter { matches(it.categories, it.difficulty) }.map { p ->
                        val list = catalog.exercisesFor(p)
                        ProgramCardModel(p, list.firstOrNull(), list.size)
                    }
                }
            LibraryUiState(false, catalog.exercises.size, programs, exercises, f)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    init {
        viewModelScope.launch { repository.refreshCatalog() }
    }

    fun select(value: LibraryFilter) {
        filter.value = value
    }

    fun setQuery(value: String) {
        _query.value = value.take(40)
    }
}

data class ProgramDetailUiState(
    val loading: Boolean = true,
    val program: WorkoutProgram? = null,
    val exercises: List<Exercise> = emptyList(),
)

class ProgramDetailViewModel(
    repository: WorkoutRepository,
    programId: String,
) : ViewModel() {
    val state: StateFlow<ProgramDetailUiState> =
        repository
            .observeCatalog()
            .map { catalog ->
                val program = catalog.program(programId)
                ProgramDetailUiState(false, program, program?.let { catalog.exercisesFor(it) }.orEmpty())
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgramDetailUiState())

    init {
        // GET /workouts/{id}: refresh this program; the catalog flow picks up any change.
        viewModelScope.launch { repository.fetchProgram(programId) }
    }
}

data class ExerciseDetailUiState(
    val loading: Boolean = true,
    val exercise: Exercise? = null,
)

class ExerciseDetailViewModel(
    repository: WorkoutRepository,
    exerciseId: String,
) : ViewModel() {
    val state: StateFlow<ExerciseDetailUiState> =
        repository
            .observeCatalog()
            .map { ExerciseDetailUiState(false, it.exercise(exerciseId)) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseDetailUiState())
}
