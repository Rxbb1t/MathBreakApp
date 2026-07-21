package com.ak.momapp.data

import com.ak.momapp.problem.LevelLadder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SkipTallyTest {

    /** Skips [n] times in a row and returns what each one cost. */
    private fun run(n: Int): List<Int> {
        var streak = 0
        return List(n) {
            val (next, penalty) = SkipTally.record(streak)
            streak = next
            penalty
        }
    }

    @Test
    fun `the first two skips in a row are free`() {
        assertEquals(listOf(0, 0), run(2))
    }

    @Test
    fun `the third in a row is the one that costs`() {
        assertEquals(listOf(0, 0, LevelLadder.STEP_SKIP), run(3))
    }

    @Test
    fun `six in a row costs more than three did`() {
        val costs = run(6)
        assertEquals(LevelLadder.STEP_SKIP, costs[2])
        assertEquals(LevelLadder.STEP_SKIP_PERSISTENT, costs[5])
        assertTrue("six should sting more than three", costs[5] > costs[2])
    }

    @Test
    fun `it stays at the higher price beyond six`() {
        val costs = run(12)
        assertEquals(LevelLadder.STEP_SKIP_PERSISTENT, costs[8])
        assertEquals(LevelLadder.STEP_SKIP_PERSISTENT, costs[11])
    }

    @Test
    fun `only every third skip costs anything`() {
        val costs = run(12)
        assertEquals(12 / LevelLadder.SKIPS_BEFORE_PENALTY, costs.count { it > 0 })
    }

    /**
     * The run is what matters, not the total. Answering something resets
     * the streak, and the repository does that for any non-skip outcome.
     */
    @Test
    fun `a broken run starts the count again`() {
        var streak = 0
        repeat(2) { streak = SkipTally.record(streak).first }
        streak = 0 // she answered something
        val (_, penalty) = SkipTally.record(streak)
        assertEquals("the next skip should be free again", 0, penalty)
    }

    @Test
    fun `no skip ever costs more than the worst single answer`() {
        assertTrue(run(30).max() <= LevelLadder.MAX_DROP)
    }
}
