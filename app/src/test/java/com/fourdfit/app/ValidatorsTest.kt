package com.fourdfit.app

import com.fourdfit.app.utils.Units
import com.fourdfit.app.utils.Validators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ValidatorsTest {
    private val today = LocalDate.of(2026, 10, 4)

    @Test
    fun emailValidation() {
        assertNull(Validators.email("alex@example.com"))
        assertNotNull(Validators.email("alex@example"))
        assertNotNull(Validators.email("not an email"))
    }

    @Test
    fun passwordNeedsLetterAndDigit() {
        assertNull(Validators.newPassword("strongPass1"))
        assertNotNull(Validators.newPassword("short1"))
        assertNotNull(Validators.newPassword("onlyletters"))
        assertNotNull(Validators.newPassword("12345678"))
    }

    @Test
    fun dateOfBirthEnforcesMinimumAge() {
        assertNotNull(Validators.dateOfBirth(null, today))
        assertNotNull(Validators.dateOfBirth(today.plusDays(1), today))
        assertNotNull(Validators.dateOfBirth(today.minusYears(12), today))
        assertNull(Validators.dateOfBirth(today.minusYears(13), today))
    }

    @Test
    fun nameAndHeight() {
        assertNull(Validators.name("Priya Sharma"))
        assertNull(Validators.name("Jean-Luc O'Neil"))
        assertNotNull(Validators.name("A"))
        assertNotNull(Validators.name("R2D2"))
        assertNull(Validators.heightCm(172f))
        assertNotNull(Validators.heightCm(80f))
        assertNotNull(Validators.heightCm(null))
    }

    @Test
    fun unitConversionRoundTrips() {
        assertEquals(5 to 9, Units.cmToFeetInches(175f))
        assertEquals(175.26f, Units.feetInchesToCm(5, 9), 0.01f)
    }
}
