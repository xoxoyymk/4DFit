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
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 4D FIT REST API. Paths are relative to BuildConfig.API_BASE_URL (e.g. https://api.example.com/v1/).
 * Authenticated calls carry "Authorization: Bearer <token>" (added by [AuthInterceptor]).
 * Errors return a JSON body: {"message": "..."}.
 */
interface FitApi {
    @POST("register")
    suspend fun register(
        @Body body: RegisterRequest,
    ): AuthResponse

    @POST("login")
    suspend fun login(
        @Body body: LoginRequest,
    ): AuthResponse

    @GET("profile")
    suspend fun getProfile(): UserDto

    @PUT("profile")
    suspend fun updateProfile(
        @Body body: UpdateProfileRequest,
    ): UserDto

    @DELETE("account")
    suspend fun deleteAccount(): MessageResponse

    @GET("workouts")
    suspend fun getWorkouts(): WorkoutCatalogDto

    @GET("workouts/{id}")
    suspend fun getWorkout(
        @Path("id") id: String,
    ): ProgramDto

    @GET("nutrition/30-days")
    suspend fun getNutritionPlan(): NutritionPlanDto

    @GET("horoscope/{zodiac}")
    suspend fun getHoroscope(
        @Path("zodiac") zodiac: String,
        @Query("date") date: String,
    ): HoroscopeDto

    @POST("workout/history")
    suspend fun postWorkoutHistory(
        @Body body: WorkoutHistoryRequest,
    ): WorkoutHistoryDto

    @GET("progress")
    suspend fun getProgress(): ProgressDto
}
