package com.fourdfit.app.data.mapper

import com.fourdfit.app.data.api.dto.ExerciseDto
import com.fourdfit.app.data.api.dto.HoroscopeDto
import com.fourdfit.app.data.api.dto.NutritionDayDto
import com.fourdfit.app.data.api.dto.NutritionPlanDto
import com.fourdfit.app.data.api.dto.ProgramDto
import com.fourdfit.app.data.api.dto.RegisterRequest
import com.fourdfit.app.data.api.dto.UpdateProfileRequest
import com.fourdfit.app.data.api.dto.UserDto
import com.fourdfit.app.data.api.dto.WorkoutCatalogDto
import com.fourdfit.app.data.api.dto.WorkoutHistoryDto
import com.fourdfit.app.data.api.dto.WorkoutHistoryRequest
import com.fourdfit.app.data.database.HoroscopeEntity
import com.fourdfit.app.data.database.UserEntity
import com.fourdfit.app.data.database.WorkoutHistoryEntity
import com.fourdfit.app.domain.model.ActivityLevel
import com.fourdfit.app.domain.model.Difficulty
import com.fourdfit.app.domain.model.Exercise
import com.fourdfit.app.domain.model.ExerciseCategory
import com.fourdfit.app.domain.model.FitnessGoal
import com.fourdfit.app.domain.model.Gender
import com.fourdfit.app.domain.model.Horoscope
import com.fourdfit.app.domain.model.Meal
import com.fourdfit.app.domain.model.MealType
import com.fourdfit.app.domain.model.MotionType
import com.fourdfit.app.domain.model.NutritionDay
import com.fourdfit.app.domain.model.RegistrationData
import com.fourdfit.app.domain.model.Tempo
import com.fourdfit.app.domain.model.UserProfile
import com.fourdfit.app.domain.model.WorkoutCatalog
import com.fourdfit.app.domain.model.WorkoutProgram
import com.fourdfit.app.domain.model.WorkoutSession
import com.fourdfit.app.utils.DateUtils
import com.fourdfit.app.utils.ZodiacCalculator
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

inline fun <reified E : Enum<E>> String?.toEnumOrNull(): E? =
    this?.let { value -> enumValues<E>().firstOrNull { it.name.equals(value, ignoreCase = true) } }

// ---------------------------------------------------------------- User

fun UserDto.toEntity(photoPath: String?): UserEntity? {
    val dob = DateUtils.parseIsoDate(dateOfBirth) ?: return null
    return UserEntity(
        id = id ?: return null,
        fullName = fullName.orEmpty(),
        email = email.orEmpty(),
        dateOfBirthEpochDay = dob.toEpochDay(),
        gender = gender ?: Gender.PREFER_NOT_TO_SAY.name,
        heightCm = heightCm ?: 170f,
        activityLevel = activityLevel ?: ActivityLevel.MODERATE.name,
        fitnessGoal = fitnessGoal ?: FitnessGoal.GENERAL_FITNESS.name,
        country = country.orEmpty(),
        photoPath = photoPath,
        updatedAtMillis = System.currentTimeMillis(),
    )
}

fun UserEntity.toDomain(): UserProfile =
    UserProfile(
        id = id,
        fullName = fullName,
        email = email,
        dateOfBirth = LocalDate.ofEpochDay(dateOfBirthEpochDay),
        gender = gender.toEnumOrNull<Gender>() ?: Gender.PREFER_NOT_TO_SAY,
        heightCm = heightCm,
        activityLevel = activityLevel.toEnumOrNull<ActivityLevel>() ?: ActivityLevel.MODERATE,
        fitnessGoal = fitnessGoal.toEnumOrNull<FitnessGoal>() ?: FitnessGoal.GENERAL_FITNESS,
        country = country,
        photoPath = photoPath,
    )

fun UserProfile.toEntity(): UserEntity =
    UserEntity(
        id = id,
        fullName = fullName,
        email = email,
        dateOfBirthEpochDay = dateOfBirth.toEpochDay(),
        gender = gender.name,
        heightCm = heightCm,
        activityLevel = activityLevel.name,
        fitnessGoal = fitnessGoal.name,
        country = country,
        photoPath = photoPath,
        updatedAtMillis = System.currentTimeMillis(),
    )

fun RegistrationData.toRequest(): RegisterRequest =
    RegisterRequest(
        fullName = fullName.trim(),
        email = email.trim().lowercase(),
        password = password,
        dateOfBirth = dateOfBirth.toString(),
        gender = gender.name,
        heightCm = heightCm,
        activityLevel = activityLevel.name,
        fitnessGoal = fitnessGoal.name,
        country = country,
    )

fun UserProfile.toUpdateRequest(): UpdateProfileRequest =
    UpdateProfileRequest(
        fullName = fullName.trim(),
        dateOfBirth = dateOfBirth.toString(),
        gender = gender.name,
        heightCm = heightCm,
        activityLevel = activityLevel.name,
        fitnessGoal = fitnessGoal.name,
        country = country,
    )

// ---------------------------------------------------------------- Workouts

fun ExerciseDto.toDomain(): Exercise? {
    val safeId = id ?: return null
    return Exercise(
        id = safeId,
        name = name ?: safeId,
        description = description.orEmpty(),
        categories = categories.orEmpty().mapNotNull { it.toEnumOrNull<ExerciseCategory>() },
        difficulty = difficulty.toEnumOrNull<Difficulty>() ?: Difficulty.BEGINNER,
        targetMuscles = targetMuscles.orEmpty(),
        sets = (sets ?: 1).coerceAtLeast(1),
        reps = reps,
        durationSeconds = (durationSeconds ?: 30).coerceAtLeast(5),
        restSeconds = (restSeconds ?: 30).coerceAtLeast(0),
        motion = motion.toEnumOrNull<MotionType>() ?: MotionType.BREATH,
        tempo = tempo.toEnumOrNull<Tempo>() ?: Tempo.NORMAL,
        safety = safety.orEmpty(),
        mistakes = mistakes.orEmpty(),
    )
}

fun ProgramDto.toDomain(): WorkoutProgram? {
    val safeId = id ?: return null
    return WorkoutProgram(
        id = safeId,
        title = title ?: safeId,
        subtitle = subtitle.orEmpty(),
        description = description.orEmpty(),
        categories = categories.orEmpty().mapNotNull { it.toEnumOrNull<ExerciseCategory>() },
        difficulty = difficulty.toEnumOrNull<Difficulty>() ?: Difficulty.BEGINNER,
        exerciseIds = exerciseIds.orEmpty(),
        estimatedMinutes = estimatedMinutes ?: 15,
    )
}

fun WorkoutCatalogDto.toDomain(): WorkoutCatalog =
    WorkoutCatalog(
        exercises = exercises.orEmpty().mapNotNull { it.toDomain() },
        programs = programs.orEmpty().mapNotNull { it.toDomain() },
    )

fun WorkoutSession.toEntity(
    synced: Boolean,
    remoteId: String? = null,
): WorkoutHistoryEntity =
    WorkoutHistoryEntity(
        clientId = clientId,
        remoteId = remoteId,
        workoutId = workoutId,
        title = title,
        completedAtMillis = completedAt.toEpochMilli(),
        dateEpochDay = date.toEpochDay(),
        durationSeconds = durationSeconds,
        exercisesCompleted = exercisesCompleted,
        categories = categories.joinToString(",") { it.name },
        completedFully = completedFully,
        synced = synced,
    )

fun WorkoutHistoryEntity.toDomain(): WorkoutSession =
    WorkoutSession(
        clientId = clientId,
        workoutId = workoutId,
        title = title,
        completedAt = Instant.ofEpochMilli(completedAtMillis),
        date = LocalDate.ofEpochDay(dateEpochDay),
        durationSeconds = durationSeconds,
        exercisesCompleted = exercisesCompleted,
        categories = categories.split(',').mapNotNull { it.toEnumOrNull<ExerciseCategory>() }.toSet(),
        completedFully = completedFully,
    )

fun WorkoutHistoryEntity.toRequest(): WorkoutHistoryRequest =
    WorkoutHistoryRequest(
        clientId = clientId,
        workoutId = workoutId,
        title = title,
        completedAt = Instant.ofEpochMilli(completedAtMillis).toString(),
        durationSeconds = durationSeconds,
        exercisesCompleted = exercisesCompleted,
        categories = categories.split(',').filter { it.isNotBlank() },
        completedFully = completedFully,
    )

fun WorkoutHistoryDto.toEntity(zone: ZoneId = ZoneId.systemDefault()): WorkoutHistoryEntity? {
    val instant = completedAt?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return null
    return WorkoutHistoryEntity(
        clientId = clientId ?: id ?: return null,
        remoteId = id,
        workoutId = workoutId.orEmpty(),
        title = title ?: "Workout",
        completedAtMillis = instant.toEpochMilli(),
        dateEpochDay = instant.atZone(zone).toLocalDate().toEpochDay(),
        durationSeconds = durationSeconds ?: 0,
        exercisesCompleted = exercisesCompleted ?: 0,
        categories = categories.orEmpty().joinToString(","),
        completedFully = completedFully ?: true,
        synced = true,
    )
}

// ---------------------------------------------------------------- Nutrition

private fun NutritionDayDto.meal(type: MealType): Meal {
    val dto =
        when (type) {
            MealType.BREAKFAST -> breakfast
            MealType.LUNCH -> lunch
            MealType.DINNER -> dinner
            MealType.SNACK -> snack
        }
    return Meal(type, dto?.title ?: type.label, dto?.description.orEmpty())
}

fun NutritionPlanDto.toDomain(): List<NutritionDay> =
    days
        .orEmpty()
        .mapNotNull { d ->
            val dayNumber = d.day ?: return@mapNotNull null
            NutritionDay(
                day = dayNumber,
                meals = MealType.entries.map { d.meal(it) },
                hydration = d.hydration.orEmpty(),
                wellnessTip = d.wellnessTip.orEmpty(),
                nutritionTip = d.nutritionTip.orEmpty(),
            )
        }.sortedBy { it.day }

// ---------------------------------------------------------------- Horoscope

fun HoroscopeDto.toEntity(
    fallbackSign: String,
    fallbackDate: LocalDate,
): HoroscopeEntity {
    val date = DateUtils.parseIsoDate(this.date) ?: fallbackDate
    return HoroscopeEntity(
        sign = (sign ?: fallbackSign).uppercase(),
        dateEpochDay = date.toEpochDay(),
        general = general.orEmpty(),
        motivation = motivation.orEmpty(),
        wellness = wellness.orEmpty(),
        luckyColor = luckyColor ?: "Electric blue",
        luckyColorHex = luckyColorHex ?: "#3D7BFF",
        luckyNumber = luckyNumber ?: 7,
        fetchedAtMillis = System.currentTimeMillis(),
    )
}

fun HoroscopeEntity.toDomain(): Horoscope? {
    val zodiac = ZodiacCalculator.fromName(sign) ?: return null
    return Horoscope(
        sign = zodiac,
        date = LocalDate.ofEpochDay(dateEpochDay),
        general = general,
        motivation = motivation,
        wellness = wellness,
        luckyColor = luckyColor,
        luckyColorHex = luckyColorHex,
        luckyNumber = luckyNumber,
    )
}
