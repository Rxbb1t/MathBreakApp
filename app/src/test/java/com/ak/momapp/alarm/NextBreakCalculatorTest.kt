package com.ak.momapp.alarm

import com.ak.momapp.data.BrainBreakSettings
import java.time.DayOfWeek
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NextBreakCalculatorTest {

    // Defaults: every 2 h, 9:00–17:00, Mon–Fri.
    private val settings = BrainBreakSettings()

    // 2026-07-06 is a Monday.
    private fun monday(hour: Int, minute: Int = 0): LocalDateTime =
        LocalDateTime.of(2026, 7, 6, hour, minute)

    @Test
    fun `mid-window break lands one interval later`() {
        assertEquals(monday(12, 30), NextBreakCalculator.next(monday(10, 30), settings))
    }

    @Test
    fun `break landing past the window slides to next morning`() {
        // 16:30 + 2h = 18:30, after 17:00 → Tuesday 9:00.
        assertEquals(
            LocalDateTime.of(2026, 7, 7, 9, 0),
            NextBreakCalculator.next(monday(16, 30), settings),
        )
    }

    @Test
    fun `early morning break waits for the window to open`() {
        // 6:00 + 2h = 8:00, before 9:00 → same day 9:00.
        assertEquals(monday(9, 0), NextBreakCalculator.next(monday(6, 0), settings))
    }

    @Test
    fun `friday evening skips the weekend`() {
        val fridayEvening = LocalDateTime.of(2026, 7, 10, 16, 45)
        assertEquals(
            LocalDateTime.of(2026, 7, 13, 9, 0), // Monday
            NextBreakCalculator.next(fridayEvening, settings),
        )
    }

    @Test
    fun `saturday break moves to monday morning`() {
        val saturday = LocalDateTime.of(2026, 7, 11, 11, 0)
        assertEquals(
            LocalDateTime.of(2026, 7, 13, 9, 0),
            NextBreakCalculator.next(saturday, settings),
        )
    }

    @Test
    fun `single active day is found across the week`() {
        val wednesdayOnly = settings.copy(activeDays = setOf(DayOfWeek.WEDNESDAY))
        val thursday = LocalDateTime.of(2026, 7, 9, 10, 0)
        assertEquals(
            LocalDateTime.of(2026, 7, 15, 9, 0), // next Wednesday
            NextBreakCalculator.next(thursday, wednesdayOnly),
        )
    }

    @Test
    fun `no active days means no break`() {
        assertNull(NextBreakCalculator.next(monday(10, 0), settings.copy(activeDays = emptySet())))
    }

    @Test
    fun `break exactly at window end still counts`() {
        // 15:00 + 2h = 17:00, the window's inclusive end.
        assertEquals(monday(17, 0), NextBreakCalculator.next(monday(15, 0), settings))
    }

    @Test
    fun `result is always inside the active window`() {
        val oddSettings = settings.copy(
            intervalHours = 3,
            activeStartMinutes = 10 * 60 + 15,
            activeEndMinutes = 14 * 60 + 45,
            activeDays = setOf(DayOfWeek.TUESDAY, DayOfWeek.SATURDAY),
        )
        var t = LocalDateTime.of(2026, 7, 6, 0, 0)
        repeat(24 * 14) {
            val next = NextBreakCalculator.next(t, oddSettings)!!
            assertTrue(
                "next $next for now $t is outside the window",
                NextBreakCalculator.isInActiveWindow(next, oddSettings),
            )
            assertTrue("next $next is not after now $t", next > t)
            t = t.plusHours(1)
        }
    }
}
