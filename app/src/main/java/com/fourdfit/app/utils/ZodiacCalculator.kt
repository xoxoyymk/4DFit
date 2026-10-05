package com.fourdfit.app.utils

import com.fourdfit.app.domain.model.ZodiacSign
import java.time.LocalDate
import java.time.Period

/** Western (tropical) zodiac by date of birth. For entertainment only. */
object ZodiacCalculator {
    fun signFor(date: LocalDate): ZodiacSign {
        val md = date.monthValue * 100 + date.dayOfMonth
        return when {
            md >= 1222 || md <= 119 -> ZodiacSign.CAPRICORN
            md <= 218 -> ZodiacSign.AQUARIUS
            md <= 320 -> ZodiacSign.PISCES
            md <= 419 -> ZodiacSign.ARIES
            md <= 520 -> ZodiacSign.TAURUS
            md <= 620 -> ZodiacSign.GEMINI
            md <= 722 -> ZodiacSign.CANCER
            md <= 822 -> ZodiacSign.LEO
            md <= 922 -> ZodiacSign.VIRGO
            md <= 1022 -> ZodiacSign.LIBRA
            md <= 1121 -> ZodiacSign.SCORPIO
            else -> ZodiacSign.SAGITTARIUS
        }
    }

    fun ageOn(
        dateOfBirth: LocalDate,
        today: LocalDate = LocalDate.now(),
    ): Int = Period.between(dateOfBirth, today).years

    fun fromName(name: String?): ZodiacSign? = ZodiacSign.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
}
