package com.tkriek.scrollless.util

import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateUtils {

    private val formatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun today(): LocalDate = LocalDate.now()

    fun key(date: LocalDate): String = date.format(formatter)

    fun todayKey(): String = key(today())

    fun parse(key: String): LocalDate = LocalDate.parse(key, formatter)

    /** Begin van de dag in millis, in de tijdzone van het toestel. */
    fun startOfDayMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    /** Eind van de dag (exclusief), afgekapt op "nu" voor de dag van vandaag. */
    fun endOfDayMillis(date: LocalDate): Long {
        val end = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return minOf(end, System.currentTimeMillis())
    }

    /** De 7 dagen tot en met [date], oplopend. */
    fun weekEnding(date: LocalDate = today()): List<LocalDate> =
        (6 downTo 0).map { date.minusDays(it.toLong()) }

    fun formatDuration(millis: Long): String {
        val duration = Duration.ofMillis(millis.coerceAtLeast(0))
        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60
        return when {
            hours > 0 -> "${hours}u ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "${duration.seconds}s"
        }
    }

    fun shortDayLabel(date: LocalDate): String =
        when (date.dayOfWeek.value) {
            1 -> "ma"; 2 -> "di"; 3 -> "wo"; 4 -> "do"; 5 -> "vr"; 6 -> "za"; else -> "zo"
        }
}
