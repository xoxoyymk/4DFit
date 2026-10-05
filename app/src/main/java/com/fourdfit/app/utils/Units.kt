package com.fourdfit.app.utils

import com.fourdfit.app.domain.model.UnitSystem
import kotlin.math.roundToInt

object Units {
    fun cmToFeetInches(cm: Float): Pair<Int, Int> {
        val totalInches = (cm / 2.54f).roundToInt()
        return totalInches / 12 to totalInches % 12
    }

    fun feetInchesToCm(
        feet: Int,
        inches: Int,
    ): Float = (feet * 12 + inches) * 2.54f

    fun formatHeight(
        cm: Float,
        units: UnitSystem,
    ): String =
        when (units) {
            UnitSystem.METRIC -> "${cm.roundToInt()} cm"
            UnitSystem.IMPERIAL -> cmToFeetInches(cm).let { (ft, inch) -> "$ft ft $inch in" }
        }
}
