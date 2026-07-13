package com.ak.momapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveWindowTest {

    @Test
    fun `valid picks are kept as-is`() {
        assertEquals(8 * 60 to 17 * 60, ActiveWindow.applyStart(8 * 60, 17 * 60))
        assertEquals(9 * 60 to 18 * 60, ActiveWindow.applyEnd(18 * 60, 9 * 60))
    }

    @Test
    fun `start moved past the end pushes the end forward`() {
        val (start, end) = ActiveWindow.applyStart(18 * 60, 17 * 60)
        assertEquals(18 * 60, start)
        assertEquals(19 * 60, end)
    }

    @Test
    fun `end moved before the start pulls the start back`() {
        val (start, end) = ActiveWindow.applyEnd(8 * 60, 9 * 60)
        assertEquals(7 * 60, start)
        assertEquals(8 * 60, end)
    }

    @Test
    fun `window stays inside the day at the extremes`() {
        val (lateStart, lateEnd) = ActiveWindow.applyStart(23 * 60 + 30, 17 * 60)
        assertTrue(lateEnd < ActiveWindow.MINUTES_IN_DAY)
        assertTrue(lateEnd - lateStart >= ActiveWindow.MIN_LENGTH_MINUTES)

        val (earlyStart, earlyEnd) = ActiveWindow.applyEnd(0, 9 * 60)
        assertTrue(earlyStart >= 0)
        assertTrue(earlyEnd - earlyStart >= ActiveWindow.MIN_LENGTH_MINUTES)
    }

    @Test
    fun `result is always a valid window`() {
        for (picked in 0 until ActiveWindow.MINUTES_IN_DAY step 15) {
            for (other in 0 until ActiveWindow.MINUTES_IN_DAY step 15) {
                for ((start, end) in listOf(
                    ActiveWindow.applyStart(picked, other),
                    ActiveWindow.applyEnd(picked, other),
                )) {
                    assertTrue("invalid window $start..$end", start in 0 until end)
                    assertTrue("too short $start..$end", end - start >= ActiveWindow.MIN_LENGTH_MINUTES)
                    assertTrue("past midnight $start..$end", end < ActiveWindow.MINUTES_IN_DAY)
                }
            }
        }
    }
}
