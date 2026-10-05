package com.fourdfit.app.di

import android.content.Context
import com.fourdfit.app.BuildConfig
import com.fourdfit.app.data.api.ApiFactory
import com.fourdfit.app.data.api.FitApi
import com.fourdfit.app.data.api.MockFitApi
import com.fourdfit.app.data.database.FitDatabase
import com.fourdfit.app.data.local.AssetSeedLoader
import com.fourdfit.app.data.local.ImageStorage
import com.fourdfit.app.data.local.MockServerStore
import com.fourdfit.app.data.local.SecureTokenStore
import com.fourdfit.app.data.local.SettingsRepositoryImpl
import com.fourdfit.app.data.repository.AchievementRepositoryImpl
import com.fourdfit.app.data.repository.ActivityRepositoryImpl
import com.fourdfit.app.data.repository.AuthRepositoryImpl
import com.fourdfit.app.data.repository.HoroscopeRepositoryImpl
import com.fourdfit.app.data.repository.NutritionRepositoryImpl
import com.fourdfit.app.data.repository.ProfileRepositoryImpl
import com.fourdfit.app.data.repository.SyncManager
import com.fourdfit.app.data.repository.WorkoutRepositoryImpl
import com.fourdfit.app.data.sensor.StepCounterManager
import com.fourdfit.app.domain.repository.AchievementRepository
import com.fourdfit.app.domain.repository.ActivityRepository
import com.fourdfit.app.domain.repository.AuthRepository
import com.fourdfit.app.domain.repository.HoroscopeRepository
import com.fourdfit.app.domain.repository.NutritionRepository
import com.fourdfit.app.domain.repository.ProfileRepository
import com.fourdfit.app.domain.repository.SettingsRepository
import com.fourdfit.app.domain.repository.WorkoutRepository
import com.fourdfit.app.notifications.ReminderScheduler
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Manual dependency container (kept simple on purpose; swap for Hilt if the app grows).
 * Switch between the on-device mock API and the REST backend with the Gradle property
 * `fourdfit.useMockApi` (see gradle.properties).
 */
class AppContainer(
    context: Context,
) {
    private val appContext = context.applicationContext

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val gson: Gson = Gson()

    val database: FitDatabase by lazy { FitDatabase.build(appContext) }
    val tokenStore: SecureTokenStore by lazy { SecureTokenStore(appContext) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepositoryImpl(appContext) }
    val seedLoader: AssetSeedLoader by lazy { AssetSeedLoader(appContext, gson) }
    val imageStorage: ImageStorage by lazy { ImageStorage(appContext) }
    val reminderScheduler: ReminderScheduler by lazy { ReminderScheduler(appContext) }

    val api: FitApi by lazy {
        if (BuildConfig.USE_MOCK_API) {
            MockFitApi(seedLoader, MockServerStore(appContext, gson), gson)
        } else {
            ApiFactory.create(BuildConfig.API_BASE_URL, tokenStore, BuildConfig.DEBUG, gson)
        }
    }

    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(api, tokenStore, database, settingsRepository, imageStorage, reminderScheduler)
    }
    val profileRepository: ProfileRepository by lazy { ProfileRepositoryImpl(api, database.userDao()) }
    val workoutRepository: WorkoutRepository by lazy {
        WorkoutRepositoryImpl(api, database.contentCacheDao(), database.workoutHistoryDao(), seedLoader, gson)
    }
    val nutritionRepository: NutritionRepository by lazy {
        NutritionRepositoryImpl(api, database.contentCacheDao(), database.mealCompletionDao(), seedLoader, settingsRepository, gson)
    }
    val horoscopeRepository: HoroscopeRepository by lazy { HoroscopeRepositoryImpl(api, database.horoscopeDao()) }
    val activityRepository: ActivityRepository by lazy { ActivityRepositoryImpl(database.dailyStatsDao()) }
    val achievementRepository: AchievementRepository by lazy {
        AchievementRepositoryImpl(database.achievementDao(), database.workoutHistoryDao())
    }
    val stepCounter: StepCounterManager by lazy { StepCounterManager(appContext, activityRepository, appScope) }
    val syncManager: SyncManager by lazy {
        SyncManager(profileRepository, workoutRepository, nutritionRepository, achievementRepository)
    }

    /** Keeps WorkManager reminders in step with the user's notification settings. */
    fun start() {
        appScope.launch {
            settingsRepository.settings
                .map { it.reminders }
                .distinctUntilChanged()
                .collect { reminderScheduler.sync(it) }
        }
    }
}
