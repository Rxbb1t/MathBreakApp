package com.ak.momapp.i18n

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The settings screen and the home-screen widget both name the next break
 * through here, so a wrong day boundary would be wrong in two places at
 * once. These are the three cases the formatter distinguishes.
 */
class BreakTimeFormatTest {

    /** A Tuesday, so "three days on" lands on a Friday. */
    private val today = LocalDate.of(2026, 7, 21)

    private fun at(date: LocalDate, hour: Int, minute: Int): Long =
        date.atTime(hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test
    fun `a break later today is named as today`() {
        assertEquals(
            "today at 14:30",
            formatNextBreak(at(today, 14, 30), EnglishStrings, today),
        )
    }

    @Test
    fun `the first minute of tomorrow is already tomorrow`() {
        assertEquals(
            "tomorrow at 0:00",
            formatNextBreak(at(today.plusDays(1), 0, 0), EnglishStrings, today),
        )
    }

    @Test
    fun `further out the day is named instead`() {
        assertEquals(
            "Fri 9:05",
            formatNextBreak(at(today.plusDays(3), 9, 5), EnglishStrings, today),
        )
    }

    @Test
    fun `Romanian names the same break in Romanian`() {
        assertEquals(
            "azi la 14:30",
            formatNextBreak(at(today, 14, 30), RomanianStrings, today),
        )
    }

    /** Minutes are padded so 9:05 can't read as five past nine hundred. */
    @Test
    fun `the clock pads minutes but not hours`() {
        assertEquals("9:05", formatTimeOfDay(9 * 60 + 5))
        assertEquals("0:00", formatTimeOfDay(0))
        assertEquals("23:59", formatTimeOfDay(23 * 60 + 59))
    }
}
