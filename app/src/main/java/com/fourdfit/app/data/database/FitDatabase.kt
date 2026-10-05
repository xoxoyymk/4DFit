package com.fourdfit.app.data.database

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

// ---------------------------------------------------------------- Entities

@Entity(tableName = "user_profile")
data class UserEntity(
    @PrimaryKey val id: String,
    val fullName: String,
    val email: String,
    val dateOfBirthEpochDay: Long,
    val gender: String,
    val heightCm: Float,
    val activityLevel: String,
    val fitnessGoal: String,
    val country: String,
    val photoPath: String?,
    val updatedAtMillis: Long,
)

@Entity(tableName = "workout_history")
data class WorkoutHistoryEntity(
    @PrimaryKey val clientId: String,
    val remoteId: String?,
    val workoutId: String,
    val title: String,
    val completedAtMillis: Long,
    val dateEpochDay: Long,
    val durationSeconds: Int,
    val exercisesCompleted: Int,
    /** Comma-separated ExerciseCategory names. */
    val categories: String,
    val completedFully: Boolean,
    val synced: Boolean,
)

@Entity(tableName = "meal_completion", primaryKeys = ["day", "mealType"])
data class MealCompletionEntity(
    val day: Int,
    val mealType: String,
    val completedAtMillis: Long,
)

@Entity(tableName = "daily_stats")
data class DailyStatsEntity(
    @PrimaryKey val dateEpochDay: Long,
    val waterGlasses: Int,
    val steps: Int,
)

@Entity(tableName = "horoscope_cache", primaryKeys = ["sign", "dateEpochDay"])
data class HoroscopeEntity(
    val sign: String,
    val dateEpochDay: Long,
    val general: String,
    val motivation: String,
    val wellness: String,
    val luckyColor: String,
    val luckyColorHex: String,
    val luckyNumber: Int,
    val fetchedAtMillis: Long,
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val unlockedAtMillis: Long,
)

/** Generic JSON cache for server content (workout catalog, nutrition plan). */
@Entity(tableName = "content_cache")
data class ContentCacheEntity(
    @PrimaryKey val key: String,
    val json: String,
    val updatedAtMillis: Long,
)

// ---------------------------------------------------------------- DAOs

@Dao
interface UserDao {
    @Query("SELECT * FROM user_profile LIMIT 1")
    fun observe(): Flow<UserEntity?>

    @Query("SELECT * FROM user_profile LIMIT 1")
    suspend fun get(): UserEntity?

    @Upsert
    suspend fun upsert(user: UserEntity)

    @Query("DELETE FROM user_profile")
    suspend fun clear()
}

@Dao
interface WorkoutHistoryDao {
    @Query("SELECT * FROM workout_history ORDER BY completedAtMillis DESC")
    fun observeAll(): Flow<List<WorkoutHistoryEntity>>

    @Query("SELECT * FROM workout_history ORDER BY completedAtMillis DESC")
    suspend fun getAll(): List<WorkoutHistoryEntity>

    @Query("SELECT * FROM workout_history WHERE synced = 0")
    suspend fun getUnsynced(): List<WorkoutHistoryEntity>

    @Query("SELECT COUNT(*) FROM workout_history WHERE dateEpochDay = :epochDay")
    suspend fun countOnDay(epochDay: Long): Int

    @Upsert
    suspend fun upsert(entity: WorkoutHistoryEntity)

    @Upsert
    suspend fun upsertAll(entities: List<WorkoutHistoryEntity>)

    @Query("UPDATE workout_history SET synced = 1, remoteId = :remoteId WHERE clientId = :clientId")
    suspend fun markSynced(
        clientId: String,
        remoteId: String?,
    )
}

@Dao
interface MealCompletionDao {
    @Query("SELECT * FROM meal_completion")
    fun observeAll(): Flow<List<MealCompletionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MealCompletionEntity)

    @Query("DELETE FROM meal_completion WHERE day = :day AND mealType = :mealType")
    suspend fun delete(
        day: Int,
        mealType: String,
    )
}

@Dao
interface DailyStatsDao {
    @Query("SELECT * FROM daily_stats WHERE dateEpochDay = :epochDay")
    fun observe(epochDay: Long): Flow<DailyStatsEntity?>

    @Query("SELECT * FROM daily_stats WHERE dateEpochDay BETWEEN :from AND :to ORDER BY dateEpochDay")
    fun observeRange(
        from: Long,
        to: Long,
    ): Flow<List<DailyStatsEntity>>

    @Query("INSERT OR IGNORE INTO daily_stats (dateEpochDay, waterGlasses, steps) VALUES (:epochDay, 0, 0)")
    suspend fun ensureRow(epochDay: Long)

    @Query("UPDATE daily_stats SET waterGlasses = MAX(0, MIN(:max, waterGlasses + :delta)) WHERE dateEpochDay = :epochDay")
    suspend fun addWater(
        epochDay: Long,
        delta: Int,
        max: Int,
    )

    @Query("UPDATE daily_stats SET steps = :steps WHERE dateEpochDay = :epochDay")
    suspend fun setSteps(
        epochDay: Long,
        steps: Int,
    )
}

@Dao
interface HoroscopeDao {
    @Query("SELECT * FROM horoscope_cache WHERE sign = :sign AND dateEpochDay = :epochDay")
    fun observe(
        sign: String,
        epochDay: Long,
    ): Flow<HoroscopeEntity?>

    @Query("SELECT * FROM horoscope_cache WHERE sign = :sign AND dateEpochDay = :epochDay")
    suspend fun get(
        sign: String,
        epochDay: Long,
    ): HoroscopeEntity?

    @Upsert
    suspend fun upsert(entity: HoroscopeEntity)

    @Query("DELETE FROM horoscope_cache WHERE dateEpochDay < :epochDay")
    suspend fun deleteOlderThan(epochDay: Long)
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements")
    fun observeAll(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements")
    suspend fun getAll(): List<AchievementEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entities: List<AchievementEntity>)
}

@Dao
interface ContentCacheDao {
    @Query("SELECT * FROM content_cache WHERE `key` = :key")
    fun observe(key: String): Flow<ContentCacheEntity?>

    @Query("SELECT * FROM content_cache WHERE `key` = :key")
    suspend fun get(key: String): ContentCacheEntity?

    @Upsert
    suspend fun upsert(entity: ContentCacheEntity)
}

// ---------------------------------------------------------------- Database

@Database(
    entities = [
        UserEntity::class,
        WorkoutHistoryEntity::class,
        MealCompletionEntity::class,
        DailyStatsEntity::class,
        HoroscopeEntity::class,
        AchievementEntity::class,
        ContentCacheEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class FitDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    abstract fun workoutHistoryDao(): WorkoutHistoryDao

    abstract fun mealCompletionDao(): MealCompletionDao

    abstract fun dailyStatsDao(): DailyStatsDao

    abstract fun horoscopeDao(): HoroscopeDao

    abstract fun achievementDao(): AchievementDao

    abstract fun contentCacheDao(): ContentCacheDao

    companion object {
        fun build(context: Context): FitDatabase =
            Room
                .databaseBuilder(context.applicationContext, FitDatabase::class.java, "fourdfit.db")
                // Add proper Migrations before changing the schema in production.
                .fallbackToDestructiveMigration()
                .build()
    }
}
