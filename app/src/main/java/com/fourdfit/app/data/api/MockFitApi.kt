package com.fourdfit.app.data.api

import com.fourdfit.app.data.api.dto.AuthResponse
import com.fourdfit.app.data.api.dto.HoroscopeDto
import com.fourdfit.app.data.api.dto.LoginRequest
import com.fourdfit.app.data.api.dto.MessageResponse
import com.fourdfit.app.data.api.dto.NutritionPlanDto
import com.fourdfit.app.data.api.dto.ProgramDto
import com.fourdfit.app.data.api.dto.ProgressDto
import com.fourdfit.app.data.api.dto.RegisterRequest
import com.fourdfit.app.data.api.dto.UpdateProfileRequest
import com.fourdfit.app.data.api.dto.UserDto
import com.fourdfit.app.data.api.dto.WorkoutCatalogDto
import com.fourdfit.app.data.api.dto.WorkoutHistoryDto
import com.fourdfit.app.data.api.dto.WorkoutHistoryRequest
import com.fourdfit.app.data.local.AssetSeedLoader
import com.fourdfit.app.data.local.MockServerStore
import com.fourdfit.app.utils.DateUtils
import com.fourdfit.app.utils.ZodiacCalculator
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.random.Random

/**
 * On-device implementation of [FitApi] used when BuildConfig.USE_MOCK_API is true.
 * Behaves like the real backend (latency, JSON error bodies, HTTP status codes) so the
 * repositories and UI are exercised exactly as in production.
 *
 * Mock-mode limitation: it keeps one account per device and does not verify passwords
 * (it never stores them). Use the REST backend in /backend for real authentication.
 */
class MockFitApi(
    private val seed: AssetSeedLoader,
    private val store: MockServerStore,
    private val gson: Gson,
) : FitApi {
    private val horoscopeEngine by lazy { HoroscopeEngine(seed.horoscopePools()) }

    private suspend fun latency() = delay(Random.nextLong(250, 650))

    private fun httpError(
        code: Int,
        message: String,
    ): HttpException {
        val body = gson.toJson(mapOf("message" to message)).toResponseBody("application/json".toMediaType())
        return HttpException(Response.error<Any>(code, body))
    }

    override suspend fun register(body: RegisterRequest): AuthResponse {
        latency()
        val dob = DateUtils.parseIsoDate(body.dateOfBirth) ?: throw httpError(400, "Invalid date of birth")
        return store.transact { state ->
            if (state.user?.email.equals(body.email.trim(), ignoreCase = true)) {
                throw httpError(409, "An account with this email already exists. Sign in instead.")
            }
            val user =
                UserDto(
                    id = "usr_" + UUID.randomUUID().toString().take(12),
                    fullName = body.fullName.trim(),
                    email = body.email.trim().lowercase(),
                    dateOfBirth = body.dateOfBirth,
                    gender = body.gender,
                    heightCm = body.heightCm,
                    activityLevel = body.activityLevel,
                    fitnessGoal = body.fitnessGoal,
                    country = body.country,
                    zodiacSign = ZodiacCalculator.signFor(dob).name,
                    createdAt = Instant.now().toString(),
                )
            state.copy(user = user, history = emptyList()) to AuthResponse(token = newToken(), user = user)
        }
    }

    override suspend fun login(body: LoginRequest): AuthResponse {
        latency()
        val user = store.read().user
        if (user == null || !user.email.equals(body.email.trim(), ignoreCase = true)) {
            throw httpError(401, "No account found for this email on this device.")
        }
        return AuthResponse(token = newToken(), user = user)
    }

    override suspend fun getProfile(): UserDto {
        latency()
        return store.read().user ?: throw httpError(401, "Your session has expired. Sign in again.")
    }

    override suspend fun updateProfile(body: UpdateProfileRequest): UserDto {
        latency()
        val dob = DateUtils.parseIsoDate(body.dateOfBirth) ?: throw httpError(400, "Invalid date of birth")
        return store.transact { state ->
            val current = state.user ?: throw httpError(401, "Your session has expired. Sign in again.")
            val updated =
                current.copy(
                    fullName = body.fullName.trim(),
                    dateOfBirth = body.dateOfBirth,
                    gender = body.gender,
                    heightCm = body.heightCm,
                    activityLevel = body.activityLevel,
                    fitnessGoal = body.fitnessGoal,
                    country = body.country,
                    zodiacSign = ZodiacCalculator.signFor(dob).name,
                )
            state.copy(user = updated) to updated
        }
    }

    override suspend fun deleteAccount(): MessageResponse {
        latency()
        store.transact { _ ->
            com.fourdfit.app.data.api.dto
                .MockServerState(user = null, history = emptyList()) to Unit
        }
        return MessageResponse("Account deleted")
    }

    override suspend fun getWorkouts(): WorkoutCatalogDto {
        latency()
        return withContext(Dispatchers.IO) { seed.workouts() }
    }

    override suspend fun getWorkout(id: String): ProgramDto {
        latency()
        val catalog = withContext(Dispatchers.IO) { seed.workouts() }
        return catalog.programs.orEmpty().firstOrNull { it.id == id } ?: throw httpError(404, "Workout not found")
    }

    override suspend fun getNutritionPlan(): NutritionPlanDto {
        latency()
        return withContext(Dispatchers.IO) { seed.nutrition() }
    }

    override suspend fun getHoroscope(
        zodiac: String,
        date: String,
    ): HoroscopeDto {
        latency()
        val sign = ZodiacCalculator.fromName(zodiac) ?: throw httpError(404, "Unknown zodiac sign")
        val day = DateUtils.parseIsoDate(date) ?: LocalDate.now()
        return withContext(Dispatchers.Default) { horoscopeEngine.generate(sign, day) }
    }

    override suspend fun postWorkoutHistory(body: WorkoutHistoryRequest): WorkoutHistoryDto {
        latency()
        return store.transact { state ->
            if (state.user == null) throw httpError(401, "Your session has expired. Sign in again.")
            val history = state.history.orEmpty()
            val existing = history.firstOrNull { it.clientId == body.clientId }
            if (existing != null) {
                state to existing
            } else {
                val dto =
                    WorkoutHistoryDto(
                        id = "wh_" + UUID.randomUUID().toString().take(12),
                        clientId = body.clientId,
                        workoutId = body.workoutId,
                        title = body.title,
                        completedAt = body.completedAt,
                        durationSeconds = body.durationSeconds,
                        exercisesCompleted = body.exercisesCompleted,
                        categories = body.categories,
                        completedFully = body.completedFully,
                    )
                state.copy(history = history + dto) to dto
            }
        }
    }

    override suspend fun getProgress(): ProgressDto {
        latency()
        val state = store.read()
        if (state.user == null) throw httpError(401, "Your session has expired. Sign in again.")
        val history = state.history.orEmpty().sortedByDescending { it.completedAt }
        return ProgressDto(
            totalWorkouts = history.size,
            totalActiveSeconds = history.sumOf { it.durationSeconds ?: 0 },
            history = history,
        )
    }

    private fun newToken(): String = "mock." + UUID.randomUUID().toString().replace("-", "")
}
