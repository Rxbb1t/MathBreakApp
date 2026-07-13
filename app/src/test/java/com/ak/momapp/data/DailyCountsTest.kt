package com.ak.momapp.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyCountsTest {

    // 2026-07-08 is a Wednesday.
    private val wednesday: LocalDate = LocalDate.of(2026, 7, 8)

    @Test
    fun `encode and decode round-trip`() {
        val counts = mapOf(20_275L to 3, 20_276L to 5)
        assertEquals(counts, DailyCounts.decode(DailyCounts.encode(counts)))
    }

    @Test
    fun `null and garbage decode to empty`() {
        assertEquals(emptyMap<Long, Int>(), DailyCounts.decode(null))
        assertEquals(emptyMap<Long, Int>(), DailyCounts.decode(""))
        assertEquals(emptyMap<Long, Int>(), DailyCounts.decode("not:a:number,uh oh"))
    }

    @Test
    fun `increment adds today and bumps existing`() {
        var counts = DailyCounts.increment(emptyMap(), wednesday)
        counts = DailyCounts.increment(counts, wednesday)
        assertEquals(2, DailyCounts.countOn(counts, wednesday))
    }

    @Test
    fun `increment prunes entries older than two weeks`() {
        val old = wednesday.minusDays(DailyCounts.KEEP_DAYS).toEpochDay()
        val recent = wednesday.minusDays(2).toEpochDay()
        val counts = DailyCounts.increment(mapOf(old to 9, recent to 4), wednesday)
        assertTrue(old !in counts)
        assertEquals(4, counts[recent])
    }

    @Test
    fun `week total sums monday through today only`() {
        val monday = LocalDate.of(2026, 7, 6)
        val counts = mapOf(
            monday.minusDays(1).toEpochDay() to 100, // last week's Sunday
            monday.toEpochDay() to 2,
            wednesday.toEpochDay() to 3,
            wednesday.plusDays(1).toEpochDay() to 50, // future (clock games)
        )
        assertEquals(5, DailyCounts.weekTotal(counts, wednesday))
    }

    @Test
    fun `monday starts the week at its own count`() {
        val monday = LocalDate.of(2026, 7, 6)
        val counts = mapOf(
            monday.minusDays(2).toEpochDay() to 7, // Saturday before
            monday.toEpochDay() to 1,
        )
        assertEquals(1, DailyCounts.weekTotal(counts, monday))
    }
}
