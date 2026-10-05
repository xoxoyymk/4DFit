package com.fourdfit.app.domain.model

import java.time.LocalDate

/** Rotating, achievable daily wellness goals. No calorie targets or restrictive advice. */
object WellnessContent {
    private val dailyGoals =
        listOf(
            "Take a 10-minute walk after one of your meals",
            "Stretch for 5 minutes before bed",
            "Spend 5 minutes outside in daylight",
            "Stand up and move for a minute every hour",
            "Eat one extra portion of vegetables",
            "Take five slow breaths before lunch",
            "Go to bed at a consistent time tonight",
            "Climb a few flights of stairs today",
            "Enjoy one meal without screens",
            "Do a 2-minute posture reset at your desk",
            "Prepare tomorrow's water bottle tonight",
            "Message someone who makes you smile",
            "Try a mobility session on a rest day",
            "Write down one win from today",
        )

    fun goalFor(date: LocalDate): String = dailyGoals[Math.floorMod(date.toEpochDay(), dailyGoals.size.toLong()).toInt()]

    const val WATER_GOAL_GLASSES = 8
    const val MAX_WATER_GLASSES = 20
}
