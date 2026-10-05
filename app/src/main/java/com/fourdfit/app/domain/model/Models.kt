package com.fourdfit.app.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.Period

enum class Gender(
    val label: String,
) {
    FEMALE("Female"),
    MALE("Male"),
    NON_BINARY("Non-binary"),
    PREFER_NOT_TO_SAY("Prefer not to say"),
}

enum class ActivityLevel(
    val label: String,
    val description: String,
    val dailyActiveMinutesGoal: Int,
    val dailyStepGoal: Int,
) {
    SEDENTARY("Mostly seated", "Little planned exercise", 15, 5000),
    LIGHT("Lightly active", "Light movement 1–3 days a week", 20, 6500),
    MODERATE("Moderately active", "Exercise 3–5 days a week", 30, 8000),
    ACTIVE("Active", "Exercise 6–7 days a week", 40, 10000),
    VERY_ACTIVE("Very active", "Daily training or a physical job", 45, 12000),
}

enum class FitnessGoal(
    val label: String,
) {
    GENERAL_FITNESS("General fitness"),
    BUILD_STRENGTH("Build strength"),
    IMPROVE_ENDURANCE("Improve endurance"),
    FLEXIBILITY("Flexibility & mobility"),
    STRESS_RELIEF("More energy, less stress"),
    HEALTHY_HABITS("Build healthy habits"),
}

enum class Difficulty(
    val label: String,
    val level: Int,
) {
    BEGINNER("Beginner", 1),
    INTERMEDIATE("Intermediate", 2),
    ADVANCED("Advanced", 3),
}

enum class ExerciseCategory(
    val label: String,
) {
    FULL_BODY("Full Body"),
    UPPER_BODY("Upper Body"),
    LOWER_BODY("Lower Body"),
    CORE("Core"),
    MOBILITY("Mobility"),
    STRETCHING("Stretching"),
    CARDIO("Cardio"),
}

/** Animated demonstration types rendered by the kinetic-figure engine. */
enum class MotionType {
    SQUAT,
    JUMP_SQUAT,
    JUMPING_JACK,
    PUSH_UP,
    PLANK,
    PLANK_UP_DOWN,
    SHOULDER_TAP,
    MOUNTAIN_CLIMBER,
    HIGH_KNEES,
    LUNGE,
    GLUTE_BRIDGE,
    CRUNCH,
    CAT_COW,
    SIDE_STRETCH,
    FORWARD_FOLD,
    ARM_CIRCLES,
    OVERHEAD_PRESS,
    BURPEE,
    BREATH,
}

enum class Tempo(
    val multiplier: Float,
) {
    SLOW(1.35f),
    NORMAL(1f),
    FAST(0.75f),
}

enum class MealType(
    val label: String,
) {
    BREAKFAST("Breakfast"),
    LUNCH("Lunch"),
    DINNER("Dinner"),
    SNACK("Snack"),
}

enum class ThemeMode(
    val label: String,
) {
    SYSTEM("System"),
    DARK("Dark"),
    LIGHT("Light"),
}

enum class UnitSystem(
    val label: String,
) {
    METRIC("Metric"),
    IMPERIAL("Imperial"),
}

enum class StepStatus { ACTIVE, NEEDS_PERMISSION, UNAVAILABLE }

enum class ZodiacSign(
    val displayName: String,
    val symbol: String,
    val dateRange: String,
    val element: String,
    val description: String,
) {
    ARIES("Aries", "♈", "Mar 21 – Apr 19", "Fire", "Bold, energetic and pioneering — often the first to jump into a new challenge."),
    TAURUS("Taurus", "♉", "Apr 20 – May 20", "Earth", "Steady, patient and grounded, with a love of comfort and routine."),
    GEMINI("Gemini", "♊", "May 21 – Jun 20", "Air", "Curious, quick-witted and social, always ready to learn something new."),
    CANCER("Cancer", "♋", "Jun 21 – Jul 22", "Water", "Caring, intuitive and loyal, with a strong sense of home."),
    LEO("Leo", "♌", "Jul 23 – Aug 22", "Fire", "Warm, confident and expressive — a natural motivator for others."),
    VIRGO("Virgo", "♍", "Aug 23 – Sep 22", "Earth", "Thoughtful, organised and practical, with an eye for detail."),
    LIBRA("Libra", "♎", "Sep 23 – Oct 22", "Air", "Balanced, diplomatic and sociable, drawn to harmony and beauty."),
    SCORPIO("Scorpio", "♏", "Oct 23 – Nov 21", "Water", "Focused, determined and passionate, with real staying power."),
    SAGITTARIUS("Sagittarius", "♐", "Nov 22 – Dec 21", "Fire", "Adventurous, optimistic and free-spirited, always seeking new horizons."),
    CAPRICORN("Capricorn", "♑", "Dec 22 – Jan 19", "Earth", "Disciplined, ambitious and reliable — a patient goal-setter."),
    AQUARIUS("Aquarius", "♒", "Jan 20 – Feb 18", "Air", "Inventive, independent and open-minded, with a forward-looking spirit."),
    PISCES("Pisces", "♓", "Feb 19 – Mar 20", "Water", "Imaginative, empathetic and gentle, guided by intuition."),
}

data class UserProfile(
    val id: String,
    val fullName: String,
    val email: String,
    val dateOfBirth: LocalDate,
    val gender: Gender,
    val heightCm: Float,
    val activityLevel: ActivityLevel,
    val fitnessGoal: FitnessGoal,
    val country: String,
    val photoPath: String?,
) {
    val firstName: String get() = fullName.trim().substringBefore(' ').ifBlank { fullName }

    fun ageOn(date: LocalDate = LocalDate.now()): Int = Period.between(dateOfBirth, date).years

    val zodiacSign: ZodiacSign get() =
        com.fourdfit.app.utils.ZodiacCalculator
            .signFor(dateOfBirth)
}

data class RegistrationData(
    val fullName: String,
    val email: String,
    val password: String,
    val dateOfBirth: LocalDate,
    val gender: Gender,
    val heightCm: Float,
    val activityLevel: ActivityLevel,
    val fitnessGoal: FitnessGoal,
    val country: String,
    val photoPath: String?,
)

data class Exercise(
    val id: String,
    val name: String,
    val description: String,
    val categories: List<ExerciseCategory>,
    val difficulty: Difficulty,
    val targetMuscles: List<String>,
    val sets: Int,
    val reps: Int?,
    val durationSeconds: Int,
    val restSeconds: Int,
    val motion: MotionType,
    val tempo: Tempo,
    val safety: List<String>,
    val mistakes: List<String>,
) {
    val volumeLabel: String get() = if (reps != null) "$sets × $reps reps" else "$sets × ${durationSeconds}s"
    val totalSeconds: Int get() = sets * durationSeconds + (sets - 1).coerceAtLeast(0) * restSeconds
}

data class WorkoutProgram(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val categories: List<ExerciseCategory>,
    val difficulty: Difficulty,
    val exerciseIds: List<String>,
    val estimatedMinutes: Int,
)

data class WorkoutCatalog(
    val exercises: List<Exercise>,
    val programs: List<WorkoutProgram>,
) {
    private val byId: Map<String, Exercise> = exercises.associateBy { it.id }

    fun exercise(id: String): Exercise? = byId[id]

    fun program(id: String): WorkoutProgram? = programs.firstOrNull { it.id == id }

    fun exercisesFor(program: WorkoutProgram): List<Exercise> = program.exerciseIds.mapNotNull { byId[it] }

    companion object {
        val EMPTY = WorkoutCatalog(emptyList(), emptyList())
    }
}

data class Meal(
    val type: MealType,
    val title: String,
    val description: String,
)

data class NutritionDay(
    val day: Int,
    val meals: List<Meal>,
    val hydration: String,
    val wellnessTip: String,
    val nutritionTip: String,
)

data class MealKey(
    val day: Int,
    val type: MealType,
)

data class Horoscope(
    val sign: ZodiacSign,
    val date: LocalDate,
    val general: String,
    val motivation: String,
    val wellness: String,
    val luckyColor: String,
    val luckyColorHex: String,
    val luckyNumber: Int,
)

data class WorkoutSession(
    val clientId: String,
    val workoutId: String,
    val title: String,
    val completedAt: Instant,
    val date: LocalDate,
    val durationSeconds: Int,
    val exercisesCompleted: Int,
    val categories: Set<ExerciseCategory>,
    val completedFully: Boolean,
)

data class DailyStats(
    val date: LocalDate,
    val waterGlasses: Int,
    val steps: Int,
)

enum class AchievementType(
    val title: String,
    val description: String,
) {
    FIRST_WORKOUT("First Workout", "Complete your first workout"),
    STREAK_7("7 Day Streak", "Work out 7 days in a row"),
    TEN_WORKOUTS("10 Workouts", "Complete 10 workouts"),
    CONSISTENCY_30("30 Day Consistency", "Be active on 30 different days"),
    MOBILITY_MASTER("Mobility Master", "Complete 10 mobility or stretching sessions"),
    CARDIO_STARTER("Cardio Starter", "Complete 3 cardio sessions"),
    WORKOUT_CHAMPION("Workout Champion", "Complete 50 workouts"),
}

data class Achievement(
    val type: AchievementType,
    val unlockedAt: Instant?,
) {
    val unlocked: Boolean get() = unlockedAt != null
}

enum class ReminderType(
    val title: String,
    val description: String,
    val hour: Int,
    val minute: Int,
    val repeatHours: Long,
) {
    MORNING_WELLNESS("Morning wellness", "A gentle check-in at 7:30 AM", 7, 30, 24),
    WORKOUT("Workout reminder", "A nudge to move at 6:00 PM", 18, 0, 24),
    HYDRATION("Hydration reminder", "Every 2 hours between 9 AM and 9 PM", 9, 0, 2),
    HOROSCOPE("Daily horoscope", "When today's reading is ready, 8:00 AM", 8, 0, 24),
    MEAL("Meal reminder", "A balanced-lunch reminder at 12:30 PM", 12, 30, 24),
    STREAK("Streak reminder", "At 8:00 PM, only if you haven't moved yet", 20, 0, 24),
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val units: UnitSystem = UnitSystem.METRIC,
    val highContrast: Boolean = false,
    val reduceMotion: Boolean = false,
    val reminders: Map<ReminderType, Boolean> = ReminderType.entries.associateWith { false },
    val planStartDate: LocalDate? = null,
)

data class DayActivity(
    val date: LocalDate,
    val minutes: Int,
    val workouts: Int,
)

data class ProgressSummary(
    val totalWorkouts: Int,
    val totalActiveSeconds: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val activeDays: Int,
    val last7Days: List<DayActivity>,
    val last28Days: List<DayActivity>,
    val activeDaysThisMonth: Int,
    val minutesThisMonth: Int,
    val todayActiveSeconds: Int,
    val workoutsThisWeek: Int,
) {
    companion object {
        val EMPTY = ProgressSummary(0, 0, 0, 0, 0, emptyList(), emptyList(), 0, 0, 0, 0)
    }
}
