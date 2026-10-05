package com.fourdfit.app

import com.fourdfit.app.domain.model.AchievementType
import com.fourdfit.app.domain.model.ExerciseCategory
import com.fourdfit.app.domain.model.WorkoutSession
import com.fourdfit.app.domain.usecase.ProgressCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ProgressCalculatorTest {
    private val today = LocalDate.of(2026, 10, 4)

    private fun session(
        date: LocalDate,
        seconds: Int = 600,
        vararg categories: ExerciseCategory,
    ) = WorkoutSession(
        clientId = "$date-$seconds-${categories.joinToString()}",
        workoutId = "w",
        title = "Workout",
        completedAt = Instant.parse("2026-10-04T10:00:00Z"),
        date = date,
        durationSeconds = seconds,
        exercisesCompleted = 3,
        categories = categories.toSet(),
        completedFully = true,
    )

    @Test
    fun currentStreakCountsBackFromToday() {
        val days = setOf(today, today.minusDays(1), today.minusDays(2), today.minusDays(4))
        assertEquals(3, ProgressCalculator.currentStreak(days, today))
    }

    @Test
    fun streakSurvivesUntilEndOfDayAfterLastWorkout() {
        val days = setOf(today.minusDays(1), today.minusDays(2))
        assertEquals(2, ProgressCalculator.currentStreak(days, today))
        assertEquals(0, ProgressCalculator.currentStreak(setOf(today.minusDays(2)), today))
    }

    @Test
    fun longestStreakFindsBestRun() {
        val days = (0L..6L).map { today.minusDays(20 + it) }.toSet() + setOf(today, today.minusDays(1))
        assertEquals(7, ProgressCalculator.longestStreak(days))
    }

    @Test
    fun summaryTotalsAndWindows() {
        val history = listOf(session(today, 600), session(today, 300), session(today.minusDays(3), 1200))
        val s = ProgressCalculator.summarize(history, today)
        assertEquals(3, s.totalWorkouts)
        assertEquals(2100, s.totalActiveSeconds)
        assertEquals(900, s.todayActiveSeconds)
        assertEquals(7, s.last7Days.size)
        assertEquals(28, s.last28Days.size)
        assertEquals(15, s.last7Days.last().minutes)
    }

    @Test
    fun achievementsUnlockAtThresholds() {
        val one = listOf(session(today))
        assertTrue(AchievementType.FIRST_WORKOUT in ProgressCalculator.earnedAchievements(one))
        assertFalse(AchievementType.TEN_WORKOUTS in ProgressCalculator.earnedAchievements(one))

        val cardio = (0L until 3L).map { session(today.minusDays(it), 600, ExerciseCategory.CARDIO) }
        assertTrue(AchievementType.CARDIO_STARTER in ProgressCalculator.earnedAchievements(cardio))

        val week = (0L until 7L).map { session(today.minusDays(it)) }
        assertTrue(AchievementType.STREAK_7 in ProgressCalculator.earnedAchievements(week))
    }
}
