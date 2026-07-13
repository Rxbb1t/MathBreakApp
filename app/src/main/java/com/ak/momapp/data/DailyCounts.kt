package com.ak.momapp.data

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Per-day solve counts as a compact string ("epochDay:count,..."), enough
 * for the stats screen without pulling in Room. Old days are pruned on
 * every increment so the value never grows.
 */
object DailyCounts {

    /** Days kept; one full week plus the previous one is all stats need. */
    const val KEEP_DAYS = 14L

    fun decode(raw: String?): Map<Long, Int> {
        if (raw.isNullOrEmpty()) return emptyMap()
        return raw.split(",").mapNotNull { entry ->
            val (day, count) = entry.split(":").takeIf { it.size == 2 } ?: return@mapNotNull null
            val d = day.toLongOrNull() ?: return@mapNotNull null
            val c = count.toIntOrNull() ?: return@mapNotNull null
            d to c
        }.toMap()
    }

    fun encode(counts: Map<Long, Int>): String =
        counts.entries.sortedBy { it.key }.joinToString(",") { "${it.key}:${it.value}" }

    fun increment(counts: Map<Long, Int>, today: LocalDate): Map<Long, Int> {
        val epochDay = today.toEpochDay()
        return counts
            .filterKeys { it > epochDay - KEEP_DAYS }
            .plus(epochDay to (counts[epochDay] ?: 0) + 1)
    }

    fun countOn(counts: Map<Long, Int>, day: LocalDate): Int =
        counts[day.toEpochDay()] ?: 0

    /** Total from Monday through [today] — the week she's in right now. */
    fun weekTotal(counts: Map<Long, Int>, today: LocalDate): Int {
        val monday = today.with(DayOfWeek.MONDAY).toEpochDay()
        return counts.entries
            .filter { it.key in monday..today.toEpochDay() }
            .sumOf { it.value }
    }
}
