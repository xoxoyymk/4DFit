package com.fourdfit.app.utils

import java.time.LocalDate

/** Client-side validation. The backend re-validates everything. */
object Validators {
    const val MIN_AGE = 13
    const val MAX_AGE = 120
    const val MIN_HEIGHT_CM = 100f
    const val MAX_HEIGHT_CM = 250f

    private val EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun name(value: String): String? {
        val v = value.trim()
        return when {
            v.length < 2 -> "Enter your full name"
            v.length > 60 -> "Use 60 characters or fewer"
            !v.all { it.isLetter() || it == ' ' || it == '-' || it == '\'' || it == '.' } ->
                "Use letters, spaces, hyphens or apostrophes"
            else -> null
        }
    }

    fun email(value: String): String? = if (value.length <= 254 && EMAIL.matches(value.trim())) null else "Enter a valid email address"

    fun newPassword(value: String): String? =
        when {
            value.length < 8 -> "Use at least 8 characters"
            value.length > 128 -> "Use 128 characters or fewer"
            value.none { it.isLetter() } || value.none { it.isDigit() } -> "Include at least one letter and one number"
            else -> null
        }

    fun existingPassword(value: String): String? = if (value.isEmpty()) "Enter your password" else null

    fun dateOfBirth(
        date: LocalDate?,
        today: LocalDate = LocalDate.now(),
    ): String? {
        if (date == null) return "Choose your date of birth"
        if (date.isAfter(today)) return "Date of birth can't be in the future"
        val age = ZodiacCalculator.ageOn(date, today)
        return when {
            age < MIN_AGE -> "You need to be at least $MIN_AGE to use 4D FIT"
            age > MAX_AGE -> "Check your date of birth"
            else -> null
        }
    }

    fun heightCm(cm: Float?): String? =
        when {
            cm == null -> "Enter your height"
            cm < MIN_HEIGHT_CM || cm > MAX_HEIGHT_CM -> "Enter a height between 100 and 250 cm"
            else -> null
        }

    fun country(value: String): String? = if (value.isBlank()) "Choose your country" else null
}
