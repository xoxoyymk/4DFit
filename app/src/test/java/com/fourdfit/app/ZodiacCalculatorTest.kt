package com.fourdfit.app

import com.fourdfit.app.domain.model.ZodiacSign
import com.fourdfit.app.utils.ZodiacCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ZodiacCalculatorTest {
    private fun sign(
        month: Int,
        day: Int,
    ) = ZodiacCalculator.signFor(LocalDate.of(2000, month, day))

    @Test
    fun boundariesMatchWesternZodiac() {
        assertEquals(ZodiacSign.CAPRICORN, sign(1, 19))
        assertEquals(ZodiacSign.AQUARIUS, sign(1, 20))
        assertEquals(ZodiacSign.AQUARIUS, sign(2, 18))
        assertEquals(ZodiacSign.PISCES, sign(2, 19))
        assertEquals(ZodiacSign.PISCES, sign(3, 20))
        assertEquals(ZodiacSign.ARIES, sign(3, 21))
        assertEquals(ZodiacSign.ARIES, sign(4, 19))
        assertEquals(ZodiacSign.TAURUS, sign(4, 20))
        assertEquals(ZodiacSign.GEMINI, sign(5, 21))
        assertEquals(ZodiacSign.CANCER, sign(6, 21))
        assertEquals(ZodiacSign.LEO, sign(7, 23))
        assertEquals(ZodiacSign.LEO, sign(8, 22))
        assertEquals(ZodiacSign.VIRGO, sign(8, 23))
        assertEquals(ZodiacSign.LIBRA, sign(9, 23))
        assertEquals(ZodiacSign.SCORPIO, sign(10, 23))
        assertEquals(ZodiacSign.SAGITTARIUS, sign(11, 22))
        assertEquals(ZodiacSign.SAGITTARIUS, sign(12, 21))
        assertEquals(ZodiacSign.CAPRICORN, sign(12, 22))
    }

    @Test
    fun ageCountsCompletedYears() {
        val dob = LocalDate.of(1990, 8, 15)
        assertEquals(35, ZodiacCalculator.ageOn(dob, LocalDate.of(2026, 8, 14)))
        assertEquals(36, ZodiacCalculator.ageOn(dob, LocalDate.of(2026, 8, 15)))
    }

    @Test
    fun leapDayBirthday() {
        assertEquals(ZodiacSign.PISCES, ZodiacCalculator.signFor(LocalDate.of(2004, 2, 29)))
    }
}
