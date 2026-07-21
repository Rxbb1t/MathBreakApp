package com.ak.momapp.problem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WarmupTest {

    private val level = Level.of(60)

    @Test
    fun `the first problem is the easiest`() {
        assertEquals(Level.of(60 - Warmup.DROP), Warmup.ease(level, 0))
    }

    @Test
    fun `the easing tapers off, then stops`() {
        val first = Warmup.ease(level, 0).points
        val second = Warmup.ease(level, 1).points
        val third = Warmup.ease(level, 2).points
        assertTrue("first ($first) should be easier than second ($second)", first < second)
        assertTrue("second ($second) should still be eased", second < 60)
        assertEquals("by the third it is back to normal", 60, third)
    }

    @Test
    fun `nothing past the warm-up window is touched`() {
        for (done in Warmup.PROBLEMS..10) {
            assertEquals(level, Warmup.ease(level, done))
        }
    }

    @Test
    fun `a warm-up never falls off the bottom of the scale`() {
        val low = Level.FLOOR
        assertEquals(Level.FLOOR, Warmup.ease(low, 0))
        assertTrue(Warmup.ease(Level.of(5), 0).points >= Level.MIN)
    }

    @Test
    fun `easing only ever makes a problem easier, never harder`() {
        for (points in Level.MIN..Level.MAX) {
            val base = Level.of(points)
            for (done in 0..Warmup.PROBLEMS) {
                assertTrue(Warmup.ease(base, done) <= base)
            }
        }
    }
}
