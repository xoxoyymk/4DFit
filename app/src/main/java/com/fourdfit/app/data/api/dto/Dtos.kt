package com.fourdfit.app.data.api.dto

/*
 * JSON payloads exchanged with the backend (Gson). Response fields are nullable because Gson
 * bypasses Kotlin constructors; mappers apply safe fallbacks.
 * Dates use ISO-8601 strings ("2026-10-03", "2026-10-03T08:30:00Z").
 */

data class RegisterRequest(
    val fullName: String,
    val email: String,
    val password: String,
    val dateOfBirth: String,
    val gender: String,
    val heightCm: Float,
    val activityLevel: String,
    val fitnessGoal: String,
    val country: String,
)

data class LoginRequest(
    val email: String,
    val password: String,
)

data class AuthResponse(
    val token: String?,
    val user: UserDto?,
)

data class UserDto(
    val id: String?,
    val fullName: String?,
    val email: String?,
    val dateOfBirth: String?,
    val gender: String?,
    val heightCm: Float?,
    val activityLevel: String?,
    val fitnessGoal: String?,
    val country: String?,
    val zodiacSign: String?,
    val createdAt: String?,
)

data class UpdateProfileRequest(
    val fullName: String,
    val dateOfBirth: String,
    val gender: String,
    val heightCm: Float,
    val activityLevel: String,
    val fitnessGoal: String,
    val country: String,
)

data class MessageResponse(
    val message: String?,
)

data class WorkoutCatalogDto(
    val version: Int?,
    val exercises: List<ExerciseDto>?,
    val programs: List<ProgramDto>?,
)

data class ExerciseDto(
    val id: String?,
    val name: String?,
    val description: String?,
    val categories: List<String>?,
    val difficulty: String?,
    val targetMuscles: List<String>?,
    val sets: Int?,
    val reps: Int?,
    val durationSeconds: Int?,
    val restSeconds: Int?,
    val motion: String?,
    val tempo: String?,
    val safety: List<String>?,
    val mistakes: List<String>?,
)

data class ProgramDto(
    val id: String?,
    val title: String?,
    val subtitle: String?,
    val description: String?,
    val categories: List<String>?,
    val difficulty: String?,
    val exerciseIds: List<String>?,
    val estimatedMinutes: Int?,
)

data class NutritionPlanDto(
    val version: Int?,
    val disclaimer: String?,
    val days: List<NutritionDayDto>?,
)

data class NutritionDayDto(
    val day: Int?,
    val breakfast: MealDto?,
    val lunch: MealDto?,
    val dinner: MealDto?,
    val snack: MealDto?,
    val hydration: String?,
    val wellnessTip: String?,
    val nutritionTip: String?,
)

data class MealDto(
    val title: String?,
    val description: String?,
)

data class HoroscopeDto(
    val sign: String?,
    val date: String?,
    val general: String?,
    val motivation: String?,
    val wellness: String?,
    val luckyColor: String?,
    val luckyColorHex: String?,
    val luckyNumber: Int?,
    val disclaimer: String?,
)

data class HoroscopePoolsDto(
    val version: Int?,
    val elementOpeners: Map<String, List<String>>?,
    val general: List<String>?,
    val motivation: List<String>?,
    val wellness: List<String>?,
    val colors: List<ColorDto>?,
)

data class ColorDto(
    val name: String?,
    val hex: String?,
)

data class WorkoutHistoryRequest(
    val clientId: String,
    val workoutId: String,
    val title: String,
    val completedAt: String,
    val durationSeconds: Int,
    val exercisesCompleted: Int,
    val categories: List<String>,
    val completedFully: Boolean,
)

data class WorkoutHistoryDto(
    val id: String?,
    val clientId: String?,
    val workoutId: String?,
    val title: String?,
    val completedAt: String?,
    val durationSeconds: Int?,
    val exercisesCompleted: Int?,
    val categories: List<String>?,
    val completedFully: Boolean?,
)

data class ProgressDto(
    val totalWorkouts: Int?,
    val totalActiveSeconds: Int?,
    val history: List<WorkoutHistoryDto>?,
)

/** Persisted state of the on-device mock server (mock mode only; never contains passwords). */
data class MockServerState(
    val user: UserDto?,
    val history: List<WorkoutHistoryDto>?,
)
