package com.fourdfit.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourdfit.app.data.sensor.StepCounterManager
import com.fourdfit.app.domain.model.ActivityLevel
import com.fourdfit.app.domain.model.DailyStats
import com.fourdfit.app.domain.model.Difficulty
import com.fourdfit.app.domain.model.Exercise
import com.fourdfit.app.domain.model.ExerciseCategory
import com.fourdfit.app.domain.model.FitnessGoal
import com.fourdfit.app.domain.model.MealKey
import com.fourdfit.app.domain.model.NutritionDay
import com.fourdfit.app.domain.model.ProgressSummary
import com.fourdfit.app.domain.model.StepStatus
import com.fourdfit.app.domain.model.UserProfile
import com.fourdfit.app.domain.model.WellnessContent
import com.fourdfit.app.domain.model.WorkoutCatalog
import com.fourdfit.app.domain.model.WorkoutProgram
import com.fourdfit.app.domain.model.WorkoutSession
import com.fourdfit.app.domain.model.ZodiacSign
import com.fourdfit.app.domain.repository.ActivityRepository
import com.fourdfit.app.domain.repository.NutritionRepository
import com.fourdfit.app.domain.repository.ProfileRepository
import com.fourdfit.app.domain.repository.WorkoutRepository
import com.fourdfit.app.domain.usecase.ProgressCalculator
import com.fourdfit.app.utils.DateUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HomeUiState(
    val loading: Boolean = true,
    val firstName: String = "",
    val greeting: String = "",
    val dateLabel: String = "",
    val zodiac: ZodiacSign? = null,
    val photoPath: String? = null,
    val todaysWorkout: WorkoutProgram? = null,
    val todaysWorkoutPreview: Exercise? = null,
    val todaysWorkoutExerciseCount: Int = 0,
    val workoutDoneToday: Boolean = false,
    val activeMinutes: Int = 0,
    val activeMinutesGoal: Int = 30,
    val steps: Int = 0,
    val stepGoal: Int = 8000,
    val stepStatus: StepStatus = StepStatus.UNAVAILABLE,
    val water: Int = 0,
    val waterGoal: Int = WellnessContent.WATER_GOAL_GLASSES,
    val wellnessGoal: String = "",
    val nutritionTip: String = "",
    val hydrationTip: String = "",
    val planDay: Int = 1,
    val mealsDoneToday: Int = 0,
    val summary: ProgressSummary = ProgressSummary.EMPTY,
)

private data class DailyBundle(
    val stats: DailyStats,
    val plan: List<NutritionDay>,
    val completions: Set<MealKey>,
    val planDay: Int,
    val stepStatus: StepStatus,
)

class HomeViewModel(
    profileRepository: ProfileRepository,
    workoutRepository: WorkoutRepository,
    nutritionRepository: NutritionRepository,
    private val activityRepository: ActivityRepository,
    stepCounter: StepCounterManager,
) : ViewModel() {
    private val today: LocalDate = LocalDate.now()

    private val core =
        combine(
            profileRepository.observeProfile(),
            workoutRepository.observeCatalog(),
            workoutRepository.observeHistory(),
        ) { profile, catalog, history -> Triple(profile, catalog, history) }

    private val daily =
        combine(
            activityRepository.observeDay(today),
            nutritionRepository.observePlan(),
            nutritionRepository.observeCompletions(),
            nutritionRepository.observeCurrentDay(),
            stepCounter.status,
        ) { stats, plan, completions, day, stepStatus -> DailyBundle(stats, plan, completions, day, stepStatus) }

    val state: StateFlow<HomeUiState> =
        combine(core, daily) { (profile, catalog, history), d ->
            build(profile, catalog, history, d)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun addWater() {
        viewModelScope.launch { activityRepository.changeWater(LocalDate.now(), +1) }
    }

    fun removeWater() {
        viewModelScope.launch { activityRepository.changeWater(LocalDate.now(), -1) }
    }

    private fun build(
        profile: UserProfile?,
        catalog: WorkoutCatalog,
        history: List<WorkoutSession>,
        d: DailyBundle,
    ): HomeUiState {
        val summary = ProgressCalculator.summarize(history, today)
        val activity = profile?.activityLevel ?: ActivityLevel.MODERATE
        val program = pickTodaysWorkout(catalog, profile, today)
        val exercises = program?.let { catalog.exercisesFor(it) }.orEmpty()
        val nutritionDay = d.plan.firstOrNull { it.day == d.planDay }
        return HomeUiState(
            loading = false,
            firstName = profile?.firstName.orEmpty(),
            greeting = DateUtils.greeting(),
            dateLabel = today.format(DateUtils.longDate),
            zodiac = profile?.zodiacSign,
            photoPath = profile?.photoPath,
            todaysWorkout = program,
            todaysWorkoutPreview = exercises.firstOrNull { it.motion.name != "BREATH" },
            todaysWorkoutExerciseCount = exercises.size,
            workoutDoneToday = history.any { it.date == today },
            activeMinutes = summary.todayActiveSeconds / 60,
            activeMinutesGoal = activity.dailyActiveMinutesGoal,
            steps = d.stats.steps,
            stepGoal = activity.dailyStepGoal,
            stepStatus = d.stepStatus,
            water = d.stats.waterGlasses,
            wellnessGoal = WellnessContent.goalFor(today),
            nutritionTip = nutritionDay?.nutritionTip.orEmpty(),
            hydrationTip = nutritionDay?.hydration.orEmpty(),
            planDay = d.planDay,
            mealsDoneToday = d.completions.count { it.day == d.planDay },
            summary = summary,
        )
    }

    /** Rotates daily through programs that suit the user's goal and current activity level. */
    private fun pickTodaysWorkout(
        catalog: WorkoutCatalog,
        profile: UserProfile?,
        date: LocalDate,
    ): WorkoutProgram? {
        val preferred: Set<ExerciseCategory> =
            when (profile?.fitnessGoal) {
                FitnessGoal.BUILD_STRENGTH ->
                    setOf(
                        ExerciseCategory.UPPER_BODY,
                        ExerciseCategory.LOWER_BODY,
                        ExerciseCategory.FULL_BODY,
                        ExerciseCategory.CORE,
                    )
                FitnessGoal.IMPROVE_ENDURANCE -> setOf(ExerciseCategory.CARDIO, ExerciseCategory.FULL_BODY)
                FitnessGoal.FLEXIBILITY -> setOf(ExerciseCategory.MOBILITY, ExerciseCategory.STRETCHING, ExerciseCategory.CORE)
                FitnessGoal.STRESS_RELIEF -> setOf(ExerciseCategory.MOBILITY, ExerciseCategory.STRETCHING, ExerciseCategory.CARDIO)
                else -> ExerciseCategory.entries.toSet()
            }
        val beginnerOnly = profile?.activityLevel == ActivityLevel.SEDENTARY || profile?.activityLevel == ActivityLevel.LIGHT
        val suitable =
            catalog.programs
                .filter { p ->
                    (!beginnerOnly || p.difficulty != Difficulty.ADVANCED) && p.categories.any { it in preferred }
                }.ifEmpty { catalog.programs }
        if (suitable.isEmpty()) return null
        return suitable[Math.floorMod(date.toEpochDay(), suitable.size.toLong()).toInt()]
    }
}
