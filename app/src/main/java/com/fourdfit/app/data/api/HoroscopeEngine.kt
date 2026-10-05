package com.fourdfit.app.data.api

import com.fourdfit.app.data.api.dto.HoroscopeDto
import com.fourdfit.app.data.api.dto.HoroscopePoolsDto
import com.fourdfit.app.domain.model.ZodiacSign
import java.time.LocalDate
import kotlin.random.Random

/**
 * Generates a light-hearted daily reading from curated content pools.
 * Deterministic per (sign, date) so every user of a sign sees the same text that day.
 * Mirrors backend/src/horoscope.js. Content intentionally avoids medical, financial or
 * other high-stakes predictions.
 */
class HoroscopeEngine(
    private val pools: HoroscopePoolsDto,
) {
    fun generate(
        sign: ZodiacSign,
        date: LocalDate,
    ): HoroscopeDto {
        val rng = Random(sign.ordinal * 1_000_003L + date.toEpochDay() * 7_919L)
        val opener =
            pools.elementOpeners
                ?.get(sign.element)
                .orEmpty()
                .pick(rng, "Today is a good day to look after yourself.")
        val body = pools.general.orEmpty().pick(rng, "Small, steady steps feel rewarding today.")
        val motivation = pools.motivation.orEmpty().pick(rng, "Show up for five minutes and let momentum do the rest.")
        val wellness = pools.wellness.orEmpty().pick(rng, "Keep water nearby and take a short walk.")
        val color =
            pools.colors
                .orEmpty()
                .takeIf { it.isNotEmpty() }
                ?.random(rng)
        return HoroscopeDto(
            sign = sign.name,
            date = date.toString(),
            general = "$opener $body",
            motivation = motivation,
            wellness = wellness,
            luckyColor = color?.name ?: "Electric blue",
            luckyColorHex = color?.hex ?: "#3D7BFF",
            luckyNumber = rng.nextInt(1, 100),
            disclaimer = DISCLAIMER,
        )
    }

    private fun List<String>.pick(
        rng: Random,
        fallback: String,
    ): String = if (isEmpty()) fallback else this[rng.nextInt(size)]

    companion object {
        const val DISCLAIMER = "Horoscope content is for entertainment/general-interest purposes only."
    }
}
