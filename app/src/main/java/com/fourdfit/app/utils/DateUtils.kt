package com.fourdfit.app.utils

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {
    val longDate: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())
    val mediumDate: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
    val shortDay: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
    val dayMonth: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    val isoDate: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun greeting(time: LocalTime = LocalTime.now()): String =
        when (time.hour) {
            in 5..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }

    fun parseIsoDate(value: String?): LocalDate? = value?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    /** "1h 05m", "12m", "45s" */
    fun formatDuration(totalSeconds: Int): String {
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return when {
            h > 0 -> "${h}h ${"%02d".format(m)}m"
            m > 0 -> "${m}m"
            else -> "${s}s"
        }
    }

    /** "mm:ss" countdown format. */
    fun clock(totalSeconds: Int): String {
        val safe = totalSeconds.coerceAtLeast(0)
        return "%d:%02d".format(safe / 60, safe % 60)
    }
}
