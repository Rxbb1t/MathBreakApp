package com.ak.momapp.problem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelLadderTest {

    private fun climb(from: Int, answers: Int, pace: Pace = Pace.STEADY): Level {
        var level = Level.of(from)
        repeat(answers) { level = LevelLadder.next(level, correct = true, pace = pace) }
        return level
    }

    @Test
    fun `one right answer moves a few points, not a whole band`() {
        val after = LevelLadder.next(Level.of(50), correct = true, pace = Pace.STEADY)
        assertEquals(Level.of(50 + LevelLadder.STEP_STEADY), after)
        // Still Normal. The name only changes when the points earn it.
        assertEquals(Difficulty.MEDIUM, after.band)
    }

    /**
     * The steps are a ratio that names a target accuracy, not four
     * independent knobs. Pinning it here because getting it wrong is
     * invisible in every other test: an earlier draft targeted 56% and
     * carried a competent player from Easy to derivatives in forty
     * problems, with every other assertion still green.
     */
    @Test
    fun `the ladder targets the accuracy it claims to`() {
        val target = LevelLadder.STEP_MISS.toDouble() /
            (LevelLadder.STEP_STEADY + LevelLadder.STEP_MISS)
        assertEquals(LevelLadder.TARGET_ACCURACY, target, 0.02)
    }

    @Test
    fun `a fast answer climbs faster than a slow one`() {
        val fast = LevelLadder.next(Level.of(50), correct = true, pace = Pace.FAST)
        val steady = LevelLadder.next(Level.of(50), correct = true, pace = Pace.STEADY)
        val slow = LevelLadder.next(Level.of(50), correct = true, pace = Pace.SLOW)
        assertTrue("$fast > $steady > $slow", fast > steady && steady > slow)
    }

    /**
     * The user's standing decision, carried over from the tier ladder: one
     * wrong answer must never drop the level she is shown.
     */
    @Test
    fun `a single miss never costs a whole band`() {
        for (points in Level.MIN..Level.MAX) {
            val before = Level.of(points)
            val after = LevelLadder.next(before, correct = false, pace = Pace.STEADY)
            val band = LevelLadder.shownBand(after, previous = before.band)
            assertEquals("a miss at $points changed the name she sees", before.band, band)
        }
    }

    @Test
    fun `crossing a band takes several answers, and fewer when she is quick`() {
        val steady = generateSequence(Level.of(33)) {
            LevelLadder.next(it, correct = true, pace = Pace.STEADY)
        }.indexOfFirst { it.band == Difficulty.HARD }
        val fast = generateSequence(Level.of(33)) {
            LevelLadder.next(it, correct = true, pace = Pace.FAST)
        }.indexOfFirst { it.band == Difficulty.HARD }
        // Deliberate rather than glacial: a few sittings of getting
        // everything right, not one good afternoon.
        assertTrue("steady climb took $steady answers", steady in 12..24)
        assertTrue("fast climb ($fast) should beat steady ($steady)", fast < steady)
    }

    @Test
    fun `misses walk the level back down`() {
        var level = Level.of(70)
        repeat(4) { level = LevelLadder.next(level, correct = false, pace = Pace.STEADY) }
        assertEquals(Difficulty.MEDIUM, level.band)
    }

    @Test
    fun `the ceiling stops the climb`() {
        val ceiling = Level.of(Level.MEDIUM_TOP)
        var level = Level.of(60)
        repeat(20) { level = LevelLadder.next(level, correct = true, pace = Pace.FAST, ceiling = ceiling) }
        assertEquals(ceiling, level)
    }

    /**
     * A cap applied AFTER she has already climbed past it (picking the
     * Relaxed preset at Hard) freezes her where she is. Snatching the
     * ground back would read as punishment for changing a setting.
     */
    @Test
    fun `a late ceiling freezes rather than demotes`() {
        val ceiling = Level.of(Level.MEDIUM_TOP)
        val above = Level.of(85)
        assertEquals(above, LevelLadder.next(above, correct = true, pace = Pace.FAST, ceiling = ceiling))
        // Misses still work normally above the cap.
        assertEquals(Level.of(85 - LevelLadder.STEP_MISS), LevelLadder.next(above, correct = false, pace = Pace.STEADY, ceiling = ceiling))
    }

    @Test
    fun `the level never leaves the scale`() {
        var level = Level.of(2)
        repeat(50) { level = LevelLadder.next(level, correct = false, pace = Pace.STEADY) }
        assertEquals(Level.FLOOR, level)
        repeat(80) { level = LevelLadder.next(level, correct = true, pace = Pace.FAST) }
        assertEquals(Level.CEILING, level)
    }

    // ── The sticky band name ─────────────────────────────────────────────

    @Test
    fun `the shown name holds until the points are clear of the boundary`() {
        // Just over the line, but not far enough to be worth renaming.
        assertEquals(Difficulty.MEDIUM, LevelLadder.shownBand(Level.of(67), previous = Difficulty.MEDIUM))
        assertEquals(Difficulty.HARD, LevelLadder.shownBand(Level.of(76), previous = Difficulty.MEDIUM))
        // And the same coming back down.
        assertEquals(Difficulty.HARD, LevelLadder.shownBand(Level.of(66), previous = Difficulty.HARD))
        assertEquals(Difficulty.MEDIUM, LevelLadder.shownBand(Level.of(57), previous = Difficulty.HARD))
    }

    @Test
    fun `with no previous name the band speaks for itself`() {
        assertEquals(Difficulty.HARD, LevelLadder.shownBand(Level.of(67), previous = null))
    }

    @Test
    fun `alternating answers on a boundary do not flicker the name`() {
        var level = Level.of(Level.MEDIUM_TOP)
        var shown = Difficulty.MEDIUM
        repeat(12) { i ->
            level = LevelLadder.next(level, correct = i % 2 == 0, pace = Pace.STEADY)
            shown = LevelLadder.shownBand(level, shown)
            assertEquals("the name changed on answer $i", Difficulty.MEDIUM, shown)
        }
    }

    // ── Pace ─────────────────────────────────────────────────────────────

    @Test
    fun `pace is steady until there is enough evidence`() {
        var pace = PaceEstimate()
        repeat(PaceEstimate.EVIDENCE_NEEDED - 1) { pace = pace.record(30_000) }
        assertEquals(Pace.STEADY, pace.classify(1_000))
    }

    @Test
    fun `once she has a usual pace, fast and slow are told apart`() {
        var pace = PaceEstimate()
        repeat(PaceEstimate.EVIDENCE_NEEDED) { pace = pace.record(30_000) }
        assertEquals(Pace.FAST, pace.classify(8_000))
        assertEquals(Pace.STEADY, pace.classify(30_000))
        assertEquals(Pace.SLOW, pace.classify(90_000))
    }

    @Test
    fun `an abandoned problem never becomes her usual pace`() {
        var pace = PaceEstimate()
        repeat(PaceEstimate.EVIDENCE_NEEDED) { pace = pace.record(20_000) }
        val before = pace.typicalMs
        val after = pace.record(PaceEstimate.SANE_MAX_MS + 1)
        assertEquals(before, after.typicalMs)
        assertEquals(pace.samples, after.samples)
        // It is still judged, just not learned from.
        assertEquals(Pace.SLOW, pace.classify(PaceEstimate.SANE_MAX_MS + 1))
    }

    @Test
    fun `the usual pace follows her as she speeds up`() {
        var pace = PaceEstimate()
        repeat(10) { pace = pace.record(60_000) }
        repeat(20) { pace = pace.record(10_000) }
        assertTrue("settled at ${pace.typicalMs}", pace.typicalMs in 9_000..14_000)
    }

    @Test
    fun `an untimed answer leaves the pace alone`() {
        val pace = PaceEstimate(20_000, 9)
        assertEquals(pace, pace.record(0))
        assertEquals(Pace.STEADY, pace.classify(0))
    }
}
