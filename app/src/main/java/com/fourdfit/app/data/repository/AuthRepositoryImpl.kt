package com.fourdfit.app.data.repository

import com.fourdfit.app.data.api.FitApi
import com.fourdfit.app.data.api.dto.LoginRequest
import com.fourdfit.app.data.api.safeApiCall
import com.fourdfit.app.data.database.FitDatabase
import com.fourdfit.app.data.database.UserDao
import com.fourdfit.app.data.local.ImageStorage
import com.fourdfit.app.data.local.SecureTokenStore
import com.fourdfit.app.data.mapper.toDomain
import com.fourdfit.app.data.mapper.toEntity
import com.fourdfit.app.data.mapper.toRequest
import com.fourdfit.app.data.mapper.toUpdateRequest
import com.fourdfit.app.domain.model.RegistrationData
import com.fourdfit.app.domain.model.UserProfile
import com.fourdfit.app.domain.repository.AuthRepository
import com.fourdfit.app.domain.repository.ProfileRepository
import com.fourdfit.app.domain.repository.SettingsRepository
import com.fourdfit.app.notifications.ReminderScheduler
import com.fourdfit.app.utils.AppResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate

class AuthRepositoryImpl(
    private val api: FitApi,
    private val tokenStore: SecureTokenStore,
    private val database: FitDatabase,
    private val settings: SettingsRepository,
    private val imageStorage: ImageStorage,
    private val reminderScheduler: ReminderScheduler,
) : AuthRepository {
    override val isLoggedIn: Flow<Boolean> = tokenStore.token.map { !it.isNullOrBlank() }.distinctUntilChanged()

    override suspend fun register(data: RegistrationData): AppResult<Unit> {
        val response =
            when (val result = safeApiCall { api.register(data.toRequest()) }) {
                is AppResult.Error -> return result
                is AppResult.Success -> result.data
            }
        val token = response.token
        val entity = response.user?.toEntity(photoPath = data.photoPath)
        if (token.isNullOrBlank() || entity == null) return AppResult.Error("Unexpected response from the server.")
        // Persist the profile before the token flips the session to "logged in".
        database.userDao().upsert(entity)
        imageStorage.cleanupExcept(data.photoPath)
        settings.setPlanStart(LocalDate.now())
        tokenStore.save(token, entity.id)
        return AppResult.Success(Unit)
    }

    override suspend fun login(
        email: String,
        password: String,
    ): AppResult<Unit> {
        val response =
            when (val result = safeApiCall { api.login(LoginRequest(email.trim().lowercase(), password)) }) {
                is AppResult.Error -> return result
                is AppResult.Success -> result.data
            }
        val token = response.token
        val previousPhoto = database.userDao().get()?.photoPath
        val entity = response.user?.toEntity(photoPath = previousPhoto)
        if (token.isNullOrBlank() || entity == null) return AppResult.Error("Unexpected response from the server.")
        database.userDao().upsert(entity)
        if (settings.settings.first().planStartDate == null) settings.setPlanStart(LocalDate.now())
        tokenStore.save(token, entity.id)
        return AppResult.Success(Unit)
    }

    override suspend fun logout() {
        tokenStore.clear()
        clearLocalData()
    }

    override suspend fun deleteAccount(): AppResult<Unit> {
        val result = safeApiCall { api.deleteAccount() }
        if (result is AppResult.Error) return result
        tokenStore.clear()
        clearLocalData()
        return AppResult.Success(Unit)
    }

    /** Removes everything tied to the signed-in user from this device. */
    private suspend fun clearLocalData() {
        reminderScheduler.cancelAll()
        withContext(Dispatchers.IO) { database.clearAllTables() }
        settings.clearUserScopedData()
        imageStorage.clear()
    }
}

class ProfileRepositoryImpl(
    private val api: FitApi,
    private val userDao: UserDao,
) : ProfileRepository {
    override fun observeProfile(): Flow<UserProfile?> = userDao.observe().map { it?.toDomain() }

    override suspend fun refresh(): AppResult<Unit> {
        val dto =
            when (val result = safeApiCall { api.getProfile() }) {
                is AppResult.Error -> return result
                is AppResult.Success -> result.data
            }
        val local = userDao.get()
        val entity =
            dto.toEntity(photoPath = local?.photoPath)
                ?: return AppResult.Error("Unexpected profile data from the server.")
        userDao.upsert(entity)
        return AppResult.Success(Unit)
    }

    override suspend fun updateProfile(profile: UserProfile): AppResult<Unit> {
        val dto =
            when (val result = safeApiCall { api.updateProfile(profile.toUpdateRequest()) }) {
                is AppResult.Error -> return result
                is AppResult.Success -> result.data
            }
        // The photo is device-only, so keep the locally chosen path.
        val entity = dto.toEntity(photoPath = profile.photoPath) ?: profile.toEntity()
        userDao.upsert(entity)
        return AppResult.Success(Unit)
    }
}
