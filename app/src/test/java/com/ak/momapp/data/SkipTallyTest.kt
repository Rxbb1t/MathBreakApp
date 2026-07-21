package com.ak.momapp.data

import com.ak.momapp.problem.LevelLadder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SkipTallyTest {

    @Test
    fun `the first skip is free`() {
        val (tally, counts) = SkipTally.record(0)
        assertFalse("one skip should cost nothing", counts)
        assertEquals(1, tally)
    }

    @Test
    fun `the second skip is the one that counts`() {
        val (_, counts) = SkipTally.record(1)
        assertTrue(counts)
    }

    @Test
    fun `counting one resets the tally, so it takes another pair`() {
        val (tally, counts) = SkipTally.record(LevelLadder.SKIPS_PER_PENALTY - 1)
        assertTrue(counts)
        assertEquals("the next skip should start over", 0, tally)
    }

    /**
     * Over a long run of nothing but skips, only every
     * [LevelLadder.SKIPS_PER_PENALTY]-th one should land.
     */
    @Test
    fun `only a fraction of a skipping spree costs anything`() {
        var tally = 0
        var counted = 0
        repeat(20) {
            val (next, counts) = SkipTally.record(tally)
            tally = next
            if (counts) counted++
        }
        assertEquals(20 / LevelLadder.SKIPS_PER_PENALTY, counted)
    }
}
