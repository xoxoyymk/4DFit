package com.fourdfit.app.domain.usecase

import com.fourdfit.app.domain.model.AchievementType
import com.fourdfit.app.domain.model.DayActivity
import com.fourdfit.app.domain.model.ExerciseCategory
import com.fourdfit.app.domain.model.ProgressSummary
import com.fourdfit.app.domain.model.WorkoutSession
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Pure functions — no Android dependencies, covered by unit tests. */
object ProgressCalculator {
    fun currentStreak(
        activeDays: Set<LocalDate>,
        today: LocalDate,
    ): Int {
        var cursor =
            when {
                today in activeDays -> today
                today.minusDays(1) in activeDays -> today.minusDays(1)
                else -> return 0
            }
        var count = 0
        while (cursor in activeDays) {
            count++
            cursor = cursor.minusDays(1)
        }
        return count
    }

    fun longestStreak(activeDays: Set<LocalDate>): Int {
        if (activeDays.isEmpty()) return 0
        val sorted = activeDays.sorted()
        var best = 1
        var run = 1
        for (i in 1 until sorted.size) {
            run = if (sorted[i - 1].plusDays(1) == sorted[i]) run + 1 else 1
            if (run > best) best = run
        }
        return best
    }

    fun summarize(
        history: List<WorkoutSession>,
        today: LocalDate,
    ): ProgressSummary {
        if (history.isEmpty()) {
            return ProgressSummary.EMPTY.copy(
                last7Days = daysBack(today, 7, emptyMap()),
                last28Days = daysBack(today, 28, emptyMap()),
            )
        }
        val byDay: Map<LocalDate, List<WorkoutSession>> = history.groupBy { it.date }
        val activeDays = byDay.keys
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val thisMonth = history.filter { it.date.year == today.year && it.date.month == today.month }
        return ProgressSummary(
            totalWorkouts = history.size,
            totalActiveSeconds = history.sumOf { it.durationSeconds },
            currentStreak = currentStreak(activeDays, today),
            longestStreak = longestStreak(activeDays),
            activeDays = activeDays.size,
            last7Days = daysBack(today, 7, byDay),
            last28Days = daysBack(today, 28, byDay),
            activeDaysThisMonth = thisMonth.map { it.date }.toSet().size,
            minutesThisMonth = thisMonth.sumOf { it.durationSeconds } / 60,
            todayActiveSeconds = byDay[today].orEmpty().sumOf { it.durationSeconds },
            workoutsThisWeek = history.count { !it.date.isBefore(weekStart) && !it.date.isAfter(today) },
        )
    }

    private fun daysBack(
        today: LocalDate,
        count: Int,
        byDay: Map<LocalDate, List<WorkoutSession>>,
    ): List<DayActivity> =
        (count - 1 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val sessions = byDay[date].orEmpty()
            DayActivity(date, sessions.sumOf { it.durationSeconds } / 60, sessions.size)
        }

    /** Returns every achievement whose condition is met by the given history. */
    fun earnedAchievements(history: List<WorkoutSession>): Set<AchievementType> {
        val days = history.map { it.date }.toSet()
        val mobility =
            history.count {
                ExerciseCategory.MOBILITY in it.categories || ExerciseCategory.STRETCHING in it.categories
            }
        val cardio = history.count { ExerciseCategory.CARDIO in it.categories }
        return buildSet {
            if (history.isNotEmpty()) add(AchievementType.FIRST_WORKOUT)
            if (longestStreak(days) >= 7) add(AchievementType.STREAK_7)
            if (history.size >= 10) add(AchievementType.TEN_WORKOUTS)
            if (days.size >= 30) add(AchievementType.CONSISTENCY_30)
            if (mobility >= 10) add(AchievementType.MOBILITY_MASTER)
            if (cardio >= 3) add(AchievementType.CARDIO_STARTER)
            if (history.size >= 50) add(AchievementType.WORKOUT_CHAMPION)
        }
    }
}
