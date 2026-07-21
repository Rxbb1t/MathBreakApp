package com.ak.momapp.i18n

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle

/**
 * Naming a scheduled break: "today at 14:30", "tomorrow at 9:00", "Fri 9:00".
 *
 * Shared by the settings screen and the home-screen widget because the two
 * are read side by side. If they phrased the same break differently she
 * would reasonably read it as two different breaks.
 *
 * [today] is a parameter so the day-boundary cases can be tested without
 * waiting for midnight.
 */
fun formatNextBreak(
    epochMillis: Long,
    strings: Strings,
    today: LocalDate = LocalDate.now(),
): String {
    val dateTime = Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
    val time = formatTimeOfDay(dateTime.hour * 60 + dateTime.minute)
    return when (dateTime.toLocalDate()) {
        today -> strings.todayAt(time)
        today.plusDays(1) -> strings.tomorrowAt(time)
        else -> "${dateTime.dayOfWeek.getDisplayName(TextStyle.SHORT, strings.locale)} $time"
    }
}

/** Minutes past midnight as a clock reading: 545 -> "9:05". */
fun formatTimeOfDay(minutes: Int): String =
    "%d:%02d".format(minutes / 60, minutes % 60)
