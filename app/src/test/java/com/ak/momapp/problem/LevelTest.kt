package com.ak.momapp.problem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelTest {

    @Test
    fun `the scale is clamped at both ends`() {
        assertEquals(0, Level.of(-40).points)
        assertEquals(100, Level.of(500).points)
        assertEquals(0, Level.of(10).shift(-99).points)
        assertEquals(100, Level.of(98).shift(20).points)
    }

    @Test
    fun `each band covers a third of the scale`() {
        assertEquals(Difficulty.EASY, Level.of(0).band)
        assertEquals(Difficulty.EASY, Level.of(32).band)
        assertEquals(Difficulty.MEDIUM, Level.of(33).band)
        assertEquals(Difficulty.MEDIUM, Level.of(66).band)
        assertEquals(Difficulty.HARD, Level.of(67).band)
        assertEquals(Difficulty.HARD, Level.of(100).band)
    }

    @Test
    fun `a named level lands in the middle of its own band`() {
        for (band in Difficulty.entries) {
            assertEquals(band, band.toLevel().band)
        }
    }

    /**
     * The whole point of [Level.between]: the tuning the generators
     * already had is preserved exactly, and only the gaps are new.
     */
    @Test
    fun `the three tuned values survive at the band anchors`() {
        assertEquals(20, Difficulty.EASY.toLevel().between(20, 300, 900))
        assertEquals(300, Difficulty.MEDIUM.toLevel().between(20, 300, 900))
        assertEquals(900, Difficulty.HARD.toLevel().between(20, 300, 900))
    }

    @Test
    fun `values between the anchors are between the tuned numbers`() {
        val low = Difficulty.EASY.toLevel().between(20, 300, 900)
        val mid = Level.of(33).between(20, 300, 900)
        val high = Difficulty.MEDIUM.toLevel().between(20, 300, 900)
        assertTrue("$low < $mid < $high", low < mid && mid < high)
    }

    @Test
    fun `the curve holds flat outside the outer anchors`() {
        assertEquals(20, Level.of(0).between(20, 300, 900))
        assertEquals(900, Level.of(100).between(20, 300, 900))
    }

    @Test
    fun `the curve never goes backwards as the level climbs`() {
        var previous = Int.MIN_VALUE
        for (points in Level.MIN..Level.MAX) {
            val value = Level.of(points).between(5, 60, 400)
            assertTrue("dipped at $points", value >= previous)
            previous = value
        }
    }

    @Test
    fun `a span interpolates both ends and never inverts`() {
        for (points in Level.MIN..Level.MAX) {
            val span = Level.of(points).span(2..9, 10..99, 100..999)
            assertTrue("inverted at $points: $span", span.last >= span.first)
        }
        assertEquals(10..99, Difficulty.MEDIUM.toLevel().span(2..9, 10..99, 100..999))
    }

    @Test
    fun `a ramp fades a shape in across its window`() {
        val below = Level.of(40)
        val inside = Level.of(60)
        val above = Level.of(90)
        assertEquals(0.0, below.ramp(50, 80), 0.001)
        assertEquals(1.0, above.ramp(50, 80), 0.001)
        assertTrue(inside.ramp(50, 80) in 0.3..0.4)
        // Fade is the mirror image, so a pair of them always sums to one.
        assertEquals(1.0, inside.ramp(50, 80) + inside.fade(50, 80), 0.001)
    }
}
