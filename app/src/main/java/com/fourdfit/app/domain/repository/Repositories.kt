package com.fourdfit.app.domain.repository

import com.fourdfit.app.domain.model.Achievement
import com.fourdfit.app.domain.model.AchievementType
import com.fourdfit.app.domain.model.AppSettings
import com.fourdfit.app.domain.model.DailyStats
import com.fourdfit.app.domain.model.Horoscope
import com.fourdfit.app.domain.model.MealKey
import com.fourdfit.app.domain.model.MealType
import com.fourdfit.app.domain.model.NutritionDay
import com.fourdfit.app.domain.model.RegistrationData
import com.fourdfit.app.domain.model.ReminderType
import com.fourdfit.app.domain.model.ThemeMode
import com.fourdfit.app.domain.model.UnitSystem
import com.fourdfit.app.domain.model.UserProfile
import com.fourdfit.app.domain.model.WorkoutCatalog
import com.fourdfit.app.domain.model.WorkoutProgram
import com.fourdfit.app.domain.model.WorkoutSession
import com.fourdfit.app.domain.model.ZodiacSign
import com.fourdfit.app.utils.AppResult
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Contracts the UI layer depends on. Implementations live in data/repository and can be
 * swapped (mock API, REST backend, or Firebase — see docs/FIREBASE.md) without touching UI code.
 */
interface AuthRepository {
    val isLoggedIn: Flow<Boolean>

    suspend fun register(data: RegistrationData): AppResult<Unit>

    suspend fun login(
        email: String,
        password: String,
    ): AppResult<Unit>

    suspend fun logout()

    suspend fun deleteAccount(): AppResult<Unit>
}

interface ProfileRepository {
    fun observeProfile(): Flow<UserProfile?>

    suspend fun refresh(): AppResult<Unit>

    suspend fun updateProfile(profile: UserProfile): AppResult<Unit>
}

interface WorkoutRepository {
    fun observeCatalog(): Flow<WorkoutCatalog>

    fun observeHistory(): Flow<List<WorkoutSession>>

    suspend fun refreshCatalog(): AppResult<Unit>

    suspend fun fetchProgram(id: String): AppResult<WorkoutProgram>

    suspend fun refreshHistory(): AppResult<Unit>

    suspend fun recordSession(session: WorkoutSession): AppResult<Unit>

    suspend fun syncPending()
}

interface NutritionRepository {
    fun observePlan(): Flow<List<NutritionDay>>

    fun observeCompletions(): Flow<Set<MealKey>>

    /** 1-based plan day derived from the plan start date, clamped to 1..30. */
    fun observeCurrentDay(): Flow<Int>

    suspend fun setMealCompleted(
        day: Int,
        type: MealType,
        completed: Boolean,
    )

    suspend fun refresh(): AppResult<Unit>
}

interface HoroscopeRepository {
    fun observe(
        sign: ZodiacSign,
        date: LocalDate,
    ): Flow<Horoscope?>

    suspend fun refresh(
        sign: ZodiacSign,
        date: LocalDate,
        force: Boolean = false,
    ): AppResult<Unit>
}

interface ActivityRepository {
    fun observeDay(date: LocalDate): Flow<DailyStats>

    fun observeRange(
        from: LocalDate,
        to: LocalDate,
    ): Flow<List<DailyStats>>

    suspend fun changeWater(
        date: LocalDate,
        delta: Int,
    )

    suspend fun setSteps(
        date: LocalDate,
        steps: Int,
    )
}

interface AchievementRepository {
    fun observeAchievements(): Flow<List<Achievement>>

    /** Checks unlock conditions against the stored history and returns newly unlocked badges. */
    suspend fun evaluate(): List<AchievementType>
}

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setUnits(units: UnitSystem)

    suspend fun setHighContrast(enabled: Boolean)

    suspend fun setReduceMotion(enabled: Boolean)

    suspend fun setReminder(
        type: ReminderType,
        enabled: Boolean,
    )

    suspend fun setPlanStart(date: LocalDate)

    /** Clears reminders and plan progress. Appearance preferences are device-level and kept. */
    suspend fun clearUserScopedData()
}
