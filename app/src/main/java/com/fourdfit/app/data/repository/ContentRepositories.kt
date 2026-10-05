package com.fourdfit.app.data.repository

import com.fourdfit.app.data.api.FitApi
import com.fourdfit.app.data.api.dto.NutritionPlanDto
import com.fourdfit.app.data.api.dto.WorkoutCatalogDto
import com.fourdfit.app.data.api.safeApiCall
import com.fourdfit.app.data.database.AchievementDao
import com.fourdfit.app.data.database.AchievementEntity
import com.fourdfit.app.data.database.ContentCacheDao
import com.fourdfit.app.data.database.ContentCacheEntity
import com.fourdfit.app.data.database.DailyStatsDao
import com.fourdfit.app.data.database.HoroscopeDao
import com.fourdfit.app.data.database.MealCompletionDao
import com.fourdfit.app.data.database.MealCompletionEntity
import com.fourdfit.app.data.database.WorkoutHistoryDao
import com.fourdfit.app.data.local.AssetSeedLoader
import com.fourdfit.app.data.mapper.toDomain
import com.fourdfit.app.data.mapper.toEntity
import com.fourdfit.app.data.mapper.toEnumOrNull
import com.fourdfit.app.data.mapper.toRequest
import com.fourdfit.app.domain.model.Achievement
import com.fourdfit.app.domain.model.AchievementType
import com.fourdfit.app.domain.model.DailyStats
import com.fourdfit.app.domain.model.Horoscope
import com.fourdfit.app.domain.model.MealKey
import com.fourdfit.app.domain.model.MealType
import com.fourdfit.app.domain.model.NutritionDay
import com.fourdfit.app.domain.model.WellnessContent
import com.fourdfit.app.domain.model.WorkoutCatalog
import com.fourdfit.app.domain.model.WorkoutProgram
import com.fourdfit.app.domain.model.WorkoutSession
import com.fourdfit.app.domain.model.ZodiacSign
import com.fourdfit.app.domain.repository.AchievementRepository
import com.fourdfit.app.domain.repository.ActivityRepository
import com.fourdfit.app.domain.repository.HoroscopeRepository
import com.fourdfit.app.domain.repository.NutritionRepository
import com.fourdfit.app.domain.repository.ProfileRepository
import com.fourdfit.app.domain.repository.SettingsRepository
import com.fourdfit.app.domain.repository.WorkoutRepository
import com.fourdfit.app.domain.usecase.ProgressCalculator
import com.fourdfit.app.utils.AppResult
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// ======================================================================== Workouts

class WorkoutRepositoryImpl(
    private val api: FitApi,
    private val cacheDao: ContentCacheDao,
    private val historyDao: WorkoutHistoryDao,
    private val seed: AssetSeedLoader,
    private val gson: Gson,
) : WorkoutRepository {
    override fun observeCatalog(): Flow<WorkoutCatalog> =
        cacheDao
            .observe(CACHE_KEY)
            .map { cached ->
                val dto = cached?.let { runCatching { gson.fromJson(it.json, WorkoutCatalogDto::class.java) }.getOrNull() }
                val catalog = (dto ?: seed.workouts()).toDomain()
                if (catalog.exercises.isEmpty()) seed.workouts().toDomain() else catalog
            }.flowOn(Dispatchers.IO)

    override fun observeHistory(): Flow<List<WorkoutSession>> = historyDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun refreshCatalog(): AppResult<Unit> {
        val dto =
            when (val result = safeApiCall { api.getWorkouts() }) {
                is AppResult.Error -> return result
                is AppResult.Success -> result.data
            }
        if (dto.exercises.isNullOrEmpty()) return AppResult.Error("The workout library is empty.")
        withContext(Dispatchers.IO) {
            cacheDao.upsert(ContentCacheEntity(CACHE_KEY, gson.toJson(dto), System.currentTimeMillis()))
        }
        return AppResult.Success(Unit)
    }

    override suspend fun fetchProgram(id: String): AppResult<WorkoutProgram> {
        val dto =
            when (val result = safeApiCall { api.getWorkout(id) }) {
                is AppResult.Error -> return result
                is AppResult.Success -> result.data
            }
        val program = dto.toDomain() ?: return AppResult.Error("Workout not found.")
        // Merge the fresh program into the cached catalog so every screen sees the update.
        withContext(Dispatchers.IO) {
            val current =
                cacheDao.get(CACHE_KEY)?.let { runCatching { gson.fromJson(it.json, WorkoutCatalogDto::class.java) }.getOrNull() }
                    ?: seed.workouts()
            val programs = current.programs.orEmpty().map { if (it.id == id) dto else it }
            cacheDao.upsert(ContentCacheEntity(CACHE_KEY, gson.toJson(current.copy(programs = programs)), System.currentTimeMillis()))
        }
        return AppResult.Success(program)
    }

    override suspend fun refreshHistory(): AppResult<Unit> {
        val progress =
            when (val result = safeApiCall { api.getProgress() }) {
                is AppResult.Error -> return result
                is AppResult.Success -> result.data
            }
        val remote = progress.history.orEmpty().mapNotNull { it.toEntity() }
        if (remote.isNotEmpty()) historyDao.upsertAll(remote)
        return AppResult.Success(Unit)
    }

    override suspend fun recordSession(session: WorkoutSession): AppResult<Unit> {
        // Offline-first: save locally, then try to sync. Unsynced rows are retried by syncPending().
        val entity = session.toEntity(synced = false)
        historyDao.upsert(entity)
        when (val result = safeApiCall { api.postWorkoutHistory(entity.toRequest()) }) {
            is AppResult.Success -> historyDao.markSynced(session.clientId, result.data.id)
            is AppResult.Error -> Unit
        }
        return AppResult.Success(Unit)
    }

    override suspend fun syncPending() {
        historyDao.getUnsynced().forEach { entity ->
            val result = safeApiCall { api.postWorkoutHistory(entity.toRequest()) }
            if (result is AppResult.Success) historyDao.markSynced(entity.clientId, result.data.id)
        }
    }

    private companion object {
        const val CACHE_KEY = "workout_catalog"
    }
}

// ======================================================================== Nutrition

class NutritionRepositoryImpl(
    private val api: FitApi,
    private val cacheDao: ContentCacheDao,
    private val mealDao: MealCompletionDao,
    private val seed: AssetSeedLoader,
    private val settings: SettingsRepository,
    private val gson: Gson,
) : NutritionRepository {
    override fun observePlan(): Flow<List<NutritionDay>> =
        cacheDao
            .observe(CACHE_KEY)
            .map { cached ->
                val dto = cached?.let { runCatching { gson.fromJson(it.json, NutritionPlanDto::class.java) }.getOrNull() }
                val plan = (dto ?: seed.nutrition()).toDomain()
                if (plan.isEmpty()) seed.nutrition().toDomain() else plan
            }.flowOn(Dispatchers.IO)

    override fun observeCompletions(): Flow<Set<MealKey>> =
        mealDao.observeAll().map { rows ->
            rows.mapNotNull { row -> row.mealType.toEnumOrNull<MealType>()?.let { MealKey(row.day, it) } }.toSet()
        }

    override fun observeCurrentDay(): Flow<Int> =
        settings.settings
            .map { s ->
                val start = s.planStartDate ?: LocalDate.now()
                (ChronoUnit.DAYS.between(start, LocalDate.now()).toInt() + 1).coerceIn(1, PLAN_DAYS)
            }.distinctUntilChanged()

    override suspend fun setMealCompleted(
        day: Int,
        type: MealType,
        completed: Boolean,
    ) {
        if (completed) {
            mealDao.insert(MealCompletionEntity(day, type.name, System.currentTimeMillis()))
        } else {
            mealDao.delete(day, type.name)
        }
    }

    override suspend fun refresh(): AppResult<Unit> {
        val dto =
            when (val result = safeApiCall { api.getNutritionPlan() }) {
                is AppResult.Error -> return result
                is AppResult.Success -> result.data
            }
        if (dto.days.isNullOrEmpty()) return AppResult.Error("The nutrition plan is empty.")
        withContext(Dispatchers.IO) {
            cacheDao.upsert(ContentCacheEntity(CACHE_KEY, gson.toJson(dto), System.currentTimeMillis()))
        }
        return AppResult.Success(Unit)
    }

    companion object {
        const val PLAN_DAYS = 30
        private const val CACHE_KEY = "nutrition_plan"
    }
}

// ======================================================================== Horoscope

class HoroscopeRepositoryImpl(
    private val api: FitApi,
    private val dao: HoroscopeDao,
) : HoroscopeRepository {
    override fun observe(
        sign: ZodiacSign,
        date: LocalDate,
    ): Flow<Horoscope?> = dao.observe(sign.name, date.toEpochDay()).map { it?.toDomain() }

    override suspend fun refresh(
        sign: ZodiacSign,
        date: LocalDate,
        force: Boolean,
    ): AppResult<Unit> {
        if (!force && dao.get(sign.name, date.toEpochDay()) != null) return AppResult.Success(Unit)
        val dto =
            when (val result = safeApiCall { api.getHoroscope(sign.name.lowercase(), date.toString()) }) {
                is AppResult.Error -> return result
                is AppResult.Success -> result.data
            }
        dao.upsert(dto.toEntity(fallbackSign = sign.name, fallbackDate = date))
        dao.deleteOlderThan(date.minusDays(7).toEpochDay())
        return AppResult.Success(Unit)
    }
}

// ======================================================================== Daily activity

class ActivityRepositoryImpl(
    private val dao: DailyStatsDao,
) : ActivityRepository {
    override fun observeDay(date: LocalDate): Flow<DailyStats> =
        dao.observe(date.toEpochDay()).map { row ->
            DailyStats(date, row?.waterGlasses ?: 0, row?.steps ?: 0)
        }

    override fun observeRange(
        from: LocalDate,
        to: LocalDate,
    ): Flow<List<DailyStats>> =
        dao.observeRange(from.toEpochDay(), to.toEpochDay()).map { rows ->
            rows.map { DailyStats(LocalDate.ofEpochDay(it.dateEpochDay), it.waterGlasses, it.steps) }
        }

    override suspend fun changeWater(
        date: LocalDate,
        delta: Int,
    ) {
        dao.ensureRow(date.toEpochDay())
        dao.addWater(date.toEpochDay(), delta, WellnessContent.MAX_WATER_GLASSES)
    }

    override suspend fun setSteps(
        date: LocalDate,
        steps: Int,
    ) {
        dao.ensureRow(date.toEpochDay())
        dao.setSteps(date.toEpochDay(), steps.coerceAtLeast(0))
    }
}

// ======================================================================== Achievements

class AchievementRepositoryImpl(
    private val dao: AchievementDao,
    private val historyDao: WorkoutHistoryDao,
) : AchievementRepository {
    override fun observeAchievements(): Flow<List<Achievement>> =
        dao.observeAll().map { rows ->
            val unlocked = rows.associate { it.id to Instant.ofEpochMilli(it.unlockedAtMillis) }
            AchievementType.entries.map { Achievement(it, unlocked[it.name]) }
        }

    override suspend fun evaluate(): List<AchievementType> {
        val history = historyDao.getAll().map { it.toDomain() }
        val already = dao.getAll().map { it.id }.toSet()
        val earned = ProgressCalculator.earnedAchievements(history)
        val fresh = earned.filter { it.name !in already }
        if (fresh.isNotEmpty()) {
            val now = System.currentTimeMillis()
            dao.insertAll(fresh.map { AchievementEntity(it.name, now) })
        }
        return fresh.sortedBy { it.ordinal }
    }
}

// ======================================================================== Sync

/** Pulls fresh server data and pushes anything recorded offline. Failures are non-fatal. */
class SyncManager(
    private val profile: ProfileRepository,
    private val workouts: WorkoutRepository,
    private val nutrition: NutritionRepository,
    private val achievements: AchievementRepository,
) {
    suspend fun syncAll() {
        workouts.syncPending()
        profile.refresh()
        workouts.refreshCatalog()
        workouts.refreshHistory()
        nutrition.refresh()
        achievements.evaluate()
    }
}
