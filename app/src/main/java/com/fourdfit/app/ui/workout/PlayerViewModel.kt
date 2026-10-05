package com.fourdfit.app.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourdfit.app.domain.model.AchievementType
import com.fourdfit.app.domain.model.Exercise
import com.fourdfit.app.domain.model.ExerciseCategory
import com.fourdfit.app.domain.model.WorkoutSession
import com.fourdfit.app.domain.repository.AchievementRepository
import com.fourdfit.app.domain.repository.WorkoutRepository
import com.fourdfit.app.navigation.Routes
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

enum class StepKind { PREPARE, WORK, REST }

data class PlayerStep(
    val kind: StepKind,
    /** For REST steps this is the upcoming exercise. */
    val exercise: Exercise,
    val setNumber: Int,
    val totalSets: Int,
    val seconds: Int,
)

sealed interface PlayerUiState {
    data object Loading : PlayerUiState

    data class NotFound(
        val message: String,
    ) : PlayerUiState

    data class Active(
        val title: String,
        val steps: List<PlayerStep>,
        val index: Int,
        val remaining: Int,
        val paused: Boolean,
        val elapsedSeconds: Int,
    ) : PlayerUiState {
        val step: PlayerStep get() = steps[index]
        val progress: Float get() = index / steps.size.toFloat()
        val stepProgress: Float get() = remaining / step.seconds.coerceAtLeast(1).toFloat()
        val upNext: String?
            get() {
                val next = steps.drop(index + 1).firstOrNull { it.kind == StepKind.WORK } ?: return null
                return if (step.kind == StepKind.WORK && next.exercise.id == step.exercise.id) {
                    "${next.exercise.name}, set ${next.setNumber} of ${next.totalSets}"
                } else {
                    next.exercise.name
                }
            }
    }

    data class Completed(
        val title: String,
        val durationSeconds: Int,
        val exercisesCompleted: Int,
        val setsCompleted: Int,
        val completedFully: Boolean,
        val saved: Boolean,
        val newAchievements: List<AchievementType>,
    ) : PlayerUiState
}

/**
 * Drives the interactive player: get-ready countdown, then work and rest intervals for every set.
 * Pause, resume, skip and finish are supported; the session is saved offline-first on completion.
 */
class WorkoutPlayerViewModel(
    private val workoutRepository: WorkoutRepository,
    private val achievementRepository: AchievementRepository,
    private val kind: String,
    private val id: String,
) : ViewModel() {
    private val _state = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private var ticker: Job? = null
    private var workoutId = id
    private var exercises: List<Exercise> = emptyList()
    private var fallbackCategories: Set<ExerciseCategory> = emptySet()
    private var activeSeconds = 0
    private val completedWorkSteps = mutableSetOf<Int>()

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val catalog = workoutRepository.observeCatalog().first()
        val title: String
        if (kind == Routes.KIND_PROGRAM) {
            val program = catalog.program(id)
            if (program == null) {
                _state.value = PlayerUiState.NotFound("This workout isn't available.")
                return
            }
            exercises = catalog.exercisesFor(program)
            title = program.title
            workoutId = program.id
            fallbackCategories = program.categories.toSet()
        } else {
            val exercise = catalog.exercise(id)
            if (exercise == null) {
                _state.value = PlayerUiState.NotFound("This exercise isn't available.")
                return
            }
            exercises = listOf(exercise)
            title = exercise.name
            workoutId = "exercise:${exercise.id}"
            fallbackCategories = exercise.categories.toSet()
        }
        if (exercises.isEmpty()) {
            _state.value = PlayerUiState.NotFound("This workout has no exercises yet.")
            return
        }
        val steps = buildSteps(exercises)
        _state.value = PlayerUiState.Active(title, steps, 0, steps.first().seconds, paused = false, elapsedSeconds = 0)
        startTicker()
    }

    private fun buildSteps(list: List<Exercise>): List<PlayerStep> {
        val steps = mutableListOf(PlayerStep(StepKind.PREPARE, list.first(), 1, list.first().sets, PREPARE_SECONDS))
        list.forEachIndexed { i, exercise ->
            for (set in 1..exercise.sets) {
                steps += PlayerStep(StepKind.WORK, exercise, set, exercise.sets, exercise.durationSeconds)
                val lastOverall = i == list.lastIndex && set == exercise.sets
                if (!lastOverall && exercise.restSeconds > 0) {
                    val nextExercise = if (set == exercise.sets) list[i + 1] else exercise
                    val nextSet = if (set == exercise.sets) 1 else set + 1
                    steps += PlayerStep(StepKind.REST, nextExercise, nextSet, nextExercise.sets, exercise.restSeconds)
                }
            }
        }
        return steps
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker =
            viewModelScope.launch {
                while (isActive) {
                    delay(1_000)
                    tick()
                }
            }
    }

    private fun tick() {
        val s = _state.value as? PlayerUiState.Active ?: return
        if (s.paused) return
        val counted = s.step.kind != StepKind.PREPARE
        if (s.step.kind == StepKind.WORK) activeSeconds++
        val remaining = s.remaining - 1
        val elapsed = s.elapsedSeconds + if (counted) 1 else 0
        if (remaining <= 0) {
            if (s.step.kind == StepKind.WORK) completedWorkSteps += s.index
            advance(s.copy(elapsedSeconds = elapsed))
        } else {
            _state.value = s.copy(remaining = remaining, elapsedSeconds = elapsed)
        }
    }

    private fun advance(from: PlayerUiState.Active) {
        val next = from.index + 1
        if (next >= from.steps.size) {
            complete(from, fully = true)
        } else {
            _state.value = from.copy(index = next, remaining = from.steps[next].seconds)
        }
    }

    fun togglePause() {
        val s = _state.value as? PlayerUiState.Active ?: return
        _state.value = s.copy(paused = !s.paused)
    }

    fun pause() {
        val s = _state.value as? PlayerUiState.Active ?: return
        if (!s.paused) _state.value = s.copy(paused = true)
    }

    fun resume() {
        val s = _state.value as? PlayerUiState.Active ?: return
        if (s.paused) _state.value = s.copy(paused = false)
    }

    fun skip() {
        val s = _state.value as? PlayerUiState.Active ?: return
        advance(s)
    }

    fun finishEarly() {
        val s = _state.value as? PlayerUiState.Active ?: return
        complete(s, fully = false)
    }

    private fun complete(
        s: PlayerUiState.Active,
        fully: Boolean,
    ) {
        ticker?.cancel()
        val doneSteps = s.steps.filterIndexed { i, step -> step.kind == StepKind.WORK && i in completedWorkSteps }
        val doneExercises = doneSteps.map { it.exercise }.distinctBy { it.id }
        val categories = doneExercises.flatMap { it.categories }.toSet().ifEmpty { fallbackCategories }
        val shouldSave = activeSeconds >= MIN_SAVE_SECONDS
        val result =
            PlayerUiState.Completed(
                title = s.title,
                durationSeconds = s.elapsedSeconds,
                exercisesCompleted = doneExercises.size,
                setsCompleted = doneSteps.size,
                completedFully = fully,
                saved = shouldSave,
                newAchievements = emptyList(),
            )
        if (!shouldSave) {
            _state.value = result
            return
        }
        viewModelScope.launch {
            workoutRepository.recordSession(
                WorkoutSession(
                    clientId = UUID.randomUUID().toString(),
                    workoutId = workoutId,
                    title = s.title,
                    completedAt = Instant.now(),
                    date = LocalDate.now(),
                    durationSeconds = s.elapsedSeconds.coerceAtLeast(activeSeconds),
                    exercisesCompleted = doneExercises.size,
                    categories = categories,
                    completedFully = fully,
                ),
            )
            val unlocked = achievementRepository.evaluate()
            _state.value = result.copy(newAchievements = unlocked)
        }
    }

    override fun onCleared() {
        ticker?.cancel()
        super.onCleared()
    }

    private companion object {
        const val PREPARE_SECONDS = 5
        const val MIN_SAVE_SECONDS = 10
    }
}
