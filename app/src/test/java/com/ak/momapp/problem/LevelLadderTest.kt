package com.ak.momapp.problem

import kotlin.math.abs
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelLadderTest {

    private fun climb(from: Int, answers: Int, pace: Pace = Pace.STEADY): Level {
        var level = Level.of(from)
        repeat(answers) { level = LevelLadder.next(level, Outcome.CORRECT, pace) }
        return level
    }

    @Test
    fun `one right answer moves a few points, not a whole band`() {
        val after = LevelLadder.next(Level.of(50), Outcome.CORRECT, Pace.STEADY)
        val moved = after.points - 50
        assertTrue("a steady answer should nudge, not leap: moved $moved", moved in 1..5)
        // Still Normal. The name only changes when the points earn it.
        assertEquals(Difficulty.MEDIUM, after.band)
    }

    /**
     * The steps are a ratio that names a target accuracy, not a handful of
     * independent knobs. Pinning it here because getting it wrong is
     * invisible in every other test: an earlier draft targeted 56% and
     * carried a competent player from Easy to derivatives in forty
     * problems, with every other assertion still green.
     *
     * The streak bonus took away the tidy ratio. It lands on the answers
     * whose two predecessors were also clean, which at a solve rate of p is
     * p³ of them, so the balance point is now the root of a cubic. Solving
     * it beats restating it: a test that recomputed the constant the same
     * way the constant was written would agree with any mistake in it.
     */
    @Test
    fun `the ladder targets the accuracy it claims to`() {
        fun drift(p: Double) = p * LevelLadder.STEP_STEADY +
            p.pow(LevelLadder.STREAK_FOR_BONUS) * LevelLadder.STREAK_BONUS -
            (1 - p) * LevelLadder.STEP_LOST
        val target = (0..1000).map { it / 1000.0 }.minBy { abs(drift(it)) }
        assertEquals(LevelLadder.TARGET_ACCURACY, target, 0.02)
    }

    @Test
    fun `a fast answer climbs faster than a slow one`() {
        val fast = LevelLadder.next(Level.of(50), Outcome.CORRECT, Pace.FAST)
        val steady = LevelLadder.next(Level.of(50), Outcome.CORRECT, Pace.STEADY)
        val slow = LevelLadder.next(Level.of(50), Outcome.CORRECT, Pace.SLOW)
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
            val after = LevelLadder.next(before, Outcome.LOST, Pace.STEADY)
            val band = LevelLadder.shownBand(after, previous = before.band)
            assertEquals("a miss at $points changed the name she sees", before.band, band)
        }
    }

    @Test
    fun `crossing a band takes several answers, and fewer when she is quick`() {
        val steady = generateSequence(Level.of(33)) {
            LevelLadder.next(it, Outcome.CORRECT, Pace.STEADY)
        }.indexOfFirst { it.band == Difficulty.HARD }
        val fast = generateSequence(Level.of(33)) {
            LevelLadder.next(it, Outcome.CORRECT, Pace.FAST)
        }.indexOfFirst { it.band == Difficulty.HARD }
        // Deliberate rather than glacial: a few sittings of getting
        // everything right, not one good afternoon.
        assertTrue("steady climb took $steady answers", steady in 12..24)
        assertTrue("fast climb ($fast) should beat steady ($steady)", fast < steady)
    }

    @Test
    fun `misses walk the level back down`() {
        var level = Level.of(70)
        repeat(4) { level = LevelLadder.next(level, Outcome.LOST, Pace.STEADY) }
        assertEquals(Difficulty.MEDIUM, level.band)
    }

    @Test
    fun `the ceiling stops the climb`() {
        val ceiling = Level.of(Level.MEDIUM_TOP)
        var level = Level.of(60)
        repeat(20) { level = LevelLadder.next(level, Outcome.CORRECT, Pace.FAST, ceiling = ceiling) }
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
        assertEquals(above, LevelLadder.next(above, Outcome.CORRECT, Pace.FAST, ceiling = ceiling))
        // Misses still work normally above the cap.
        assertEquals(Level.of(85 - LevelLadder.STEP_LOST), LevelLadder.next(above, Outcome.LOST, Pace.STEADY, ceiling = ceiling))
    }

    @Test
    fun `the level never leaves the scale`() {
        var level = Level.of(2)
        repeat(50) { level = LevelLadder.next(level, Outcome.LOST, Pace.STEADY) }
        assertEquals(Level.FLOOR, level)
        repeat(80) { level = LevelLadder.next(level, Outcome.CORRECT, Pace.FAST) }
        assertEquals(Level.CEILING, level)
    }

    // ── What a problem is worth ──────────────────────────────────────────

    @Test
    fun `a demanding problem carries her further than a cheap one`() {
        val start = Level.of(40)
        val tap = LevelLadder.next(start, Outcome.CORRECT, Pace.STEADY, ProblemKind.TRUE_FALSE.effort)
        val equation = LevelLadder.next(start, Outcome.CORRECT, Pace.STEADY, ProblemKind.EQUATION.effort)
        assertTrue("tap $tap should trail equation $equation", tap < equation)
    }

    /**
     * The asymmetry that keeps the good exercises worth reaching for: a
     * hard problem is worth more when solved, but missing one costs no more
     * than missing anything else. Otherwise the exercises that carry her
     * fastest would also be the ones that punish her hardest, and the safe
     * play would be to avoid them.
     */
    @Test
    fun `a hard problem rewards more without costing more`() {
        val start = Level.of(50)
        val heavy = LevelLadder.next(start, Outcome.LOST, Pace.STEADY, ProblemKind.EQUATION.effort)
        val ordinary = LevelLadder.next(start, Outcome.LOST, Pace.STEADY, ProblemKind.WORD.effort)
        assertEquals(ordinary, heavy)
    }

    @Test
    fun `a cheap problem also costs less when missed`() {
        val start = Level.of(50)
        val tap = LevelLadder.next(start, Outcome.LOST, Pace.STEADY, ProblemKind.TRUE_FALSE.effort)
        val ordinary = LevelLadder.next(start, Outcome.LOST, Pace.STEADY, ProblemKind.WORD.effort)
        assertTrue("a coin-flip tap should cost less than a story", tap > ordinary)
    }

    @Test
    fun `every right answer moves her at least one point`() {
        val start = Level.of(50)
        for (kind in ProblemKind.entries) {
            val after = LevelLadder.next(start, Outcome.CORRECT, Pace.SLOW, kind.effort)
            assertTrue("$kind stalled at ${after.points}", after > start)
        }
    }

    @Test
    fun `the tap exercises are worth less than the thinking ones`() {
        val taps = listOf(ProblemKind.TRUE_FALSE, ProblemKind.COMPARE, ProblemKind.TARGET)
        val thinking = listOf(ProblemKind.LOGIC, ProblemKind.PUZZLE, ProblemKind.EQUATION)
        assertTrue(taps.maxOf { it.effort } < thinking.minOf { it.effort })
    }

    // ── Giving up, and skipping ──────────────────────────────────────────

    /**
     * THE RULE THAT MATTERS MOST HERE: being wrong is free. Only losing the
     * problem costs anything, so two stumbles ending in the right answer
     * leave her better off than when she started.
     */
    @Test
    fun `a wrong attempt costs nothing at all`() {
        val start = Level.of(50)
        assertEquals(start, LevelLadder.next(start, Outcome.WRONG, Pace.STEADY))
        for (kind in ProblemKind.entries) {
            assertEquals(
                "$kind charged for a wrong attempt",
                start,
                LevelLadder.next(start, Outcome.WRONG, Pace.STEADY, kind.effort),
            )
        }
    }

    @Test
    fun `two wrong attempts then the right answer still gains ground`() {
        var level = Level.of(50)
        level = LevelLadder.next(level, Outcome.WRONG, Pace.STEADY)
        level = LevelLadder.next(level, Outcome.WRONG, Pace.STEADY)
        level = LevelLadder.next(level, Outcome.CORRECT, Pace.STEADY)
        assertTrue("stumbling on the way should still be progress", level > Level.of(50))
    }

    @Test
    fun `losing, timing out and revealing all cost the same flat amount`() {
        val start = Level.of(50)
        val lost = LevelLadder.next(start, Outcome.LOST, Pace.STEADY)
        val gaveUp = LevelLadder.next(start, Outcome.GAVE_UP, Pace.STEADY)
        assertEquals(lost, gaveUp)
        assertEquals(Level.of(50 - LevelLadder.STEP_LOST), lost)
    }

    @Test
    fun `a skip costs a flat few points every time`() {
        val start = Level.of(50)
        val after = LevelLadder.next(start, Outcome.SKIPPED, Pace.STEADY)
        assertEquals(Level.of(50 - LevelLadder.STEP_SKIP), after)
        // And the same again, with no escalation.
        assertEquals(
            Level.of(50 - 2 * LevelLadder.STEP_SKIP),
            LevelLadder.next(after, Outcome.SKIPPED, Pace.STEADY),
        )
    }

    @Test
    fun `a skip costs less than losing the problem`() {
        val start = Level.of(50)
        val skipped = LevelLadder.next(start, Outcome.SKIPPED, Pace.STEADY)
        val lost = LevelLadder.next(start, Outcome.LOST, Pace.STEADY)
        assertTrue("a skip ($skipped) should sting less than a loss ($lost)", skipped > lost)
    }

    /**
     * Giving up is flat: it says nothing about the problem, so what the
     * problem happened to be worth should not change the price.
     */
    @Test
    fun `giving up costs the same whatever the problem was`() {
        val start = Level.of(50)
        val onATap = LevelLadder.next(start, Outcome.GAVE_UP, Pace.STEADY, ProblemKind.TRUE_FALSE.effort)
        val onAnEquation = LevelLadder.next(start, Outcome.GAVE_UP, Pace.STEADY, ProblemKind.EQUATION.effort)
        assertEquals(onATap, onAnEquation)
    }

    @Test
    fun `nothing can drop her further than one ordinary miss`() {
        val start = Level.of(60)
        for (outcome in Outcome.entries) {
            for (kind in ProblemKind.entries) {
                val after = LevelLadder.next(start, outcome, Pace.STEADY, kind.effort)
                assertTrue(
                    "$outcome on $kind dropped ${start.points - after.points}",
                    start.points - after.points <= LevelLadder.MAX_DROP,
                )
            }
        }
    }

    // ── Runs of right answers ────────────────────────────────────────────

    @Test
    fun `the first answers of a run pay nothing extra`() {
        for (streak in 0 until LevelLadder.STREAK_FOR_BONUS) {
            assertEquals(
                "a run of $streak was already paying a bonus",
                LevelLadder.next(Level.of(50), Outcome.CORRECT, Pace.STEADY),
                LevelLadder.next(Level.of(50), Outcome.CORRECT, Pace.STEADY, streak = streak),
            )
        }
    }

    @Test
    fun `from the third onward every right answer is worth two more`() {
        val plain = LevelLadder.next(Level.of(50), Outcome.CORRECT, Pace.STEADY)
        for (streak in LevelLadder.STREAK_FOR_BONUS..8) {
            val onARun = LevelLadder.next(Level.of(50), Outcome.CORRECT, Pace.STEADY, streak = streak)
            assertEquals(
                "a run of $streak paid the wrong bonus",
                plain.points + LevelLadder.STREAK_BONUS,
                onARun.points,
            )
        }
    }

    /**
     * The bonus is for the run, not for the problem, so it must not ride on
     * [Problem.effort] as well. Otherwise a streak of equations would be
     * paid the effort weighting twice over.
     */
    @Test
    fun `the bonus is the same whatever the problem was worth`() {
        val start = Level.of(40)
        for (kind in ProblemKind.entries) {
            val plain = LevelLadder.next(start, Outcome.CORRECT, Pace.STEADY, kind.effort)
            val onARun = LevelLadder.next(start, Outcome.CORRECT, Pace.STEADY, kind.effort, streak = 5)
            assertEquals(
                "$kind was paid the wrong bonus",
                LevelLadder.STREAK_BONUS,
                onARun.points - plain.points,
            )
        }
    }

    @Test
    fun `a run only pays on the way up`() {
        val start = Level.of(50)
        for (outcome in Outcome.entries - Outcome.CORRECT) {
            assertEquals(
                "$outcome was softened by a run she is no longer on",
                LevelLadder.next(start, outcome, Pace.STEADY),
                LevelLadder.next(start, outcome, Pace.STEADY, streak = 9),
            )
        }
    }

    /**
     * "Three in a row" has to mean three. A wrong attempt still costs no
     * points, but it does end the run: that is the whole difference between
     * the streak's rule and the level's.
     */
    @Test
    fun `anything but a clean solve starts the run over`() {
        var streak = 0
        repeat(4) { streak = LevelLadder.streakAfter(streak, Outcome.CORRECT) }
        assertEquals(4, streak)
        for (outcome in Outcome.entries - Outcome.CORRECT) {
            assertEquals("$outcome left the run standing", 0, LevelLadder.streakAfter(streak, outcome))
        }
    }

    @Test
    fun `a stumble then the right answer starts a new run rather than resuming`() {
        var streak = 0
        repeat(5) { streak = LevelLadder.streakAfter(streak, Outcome.CORRECT) }
        streak = LevelLadder.streakAfter(streak, Outcome.WRONG)
        streak = LevelLadder.streakAfter(streak, Outcome.CORRECT)
        assertEquals(1, streak)
        assertEquals(0, LevelLadder.bonusFor(streak))
    }

    @Test
    fun `a run gets her across a band sooner than a patchy stretch does`() {
        var onARun = Level.of(Level.EASY_TOP + 1)
        var streak = 0
        var answers = 0
        while (onARun.band != Difficulty.HARD) {
            streak = LevelLadder.streakAfter(streak, Outcome.CORRECT)
            onARun = LevelLadder.next(onARun, Outcome.CORRECT, Pace.STEADY, streak = streak)
            answers++
        }
        val patchy = generateSequence(Level.of(Level.EASY_TOP + 1)) {
            LevelLadder.next(it, Outcome.CORRECT, Pace.STEADY)
        }.indexOfFirst { it.band == Difficulty.HARD }
        assertTrue("an unbroken run ($answers) should beat a patchy one ($patchy)", answers < patchy)
        // Sooner, not instantly: crossing Normal is still a stretch of work
        // rather than one good afternoon.
        assertTrue("an unbroken run crossed Normal in only $answers answers", answers >= 10)
    }

    // ── The sticky band name ─────────────────────────────────────────────

    @Test
    fun `the shown name holds until the points are clear of the boundary`() {
        // Just over the line, but not far enough to be worth renaming.
        val intoHard = Level.MEDIUM_TOP + 1
        assertEquals(
            Difficulty.MEDIUM,
            LevelLadder.shownBand(Level.of(intoHard), previous = Difficulty.MEDIUM),
        )
        assertEquals(
            Difficulty.HARD,
            LevelLadder.shownBand(
                Level.of(intoHard + LevelLadder.BAND_HYSTERESIS),
                previous = Difficulty.MEDIUM,
            ),
        )
        // And the same coming back down.
        assertEquals(
            Difficulty.HARD,
            LevelLadder.shownBand(Level.of(Level.MEDIUM_TOP), previous = Difficulty.HARD),
        )
        assertEquals(
            Difficulty.MEDIUM,
            LevelLadder.shownBand(
                Level.of(Level.MEDIUM_TOP - LevelLadder.BAND_HYSTERESIS),
                previous = Difficulty.HARD,
            ),
        )
    }

    @Test
    fun `with no previous name the band speaks for itself`() {
        assertEquals(Difficulty.HARD, LevelLadder.shownBand(Level.of(Level.MEDIUM_TOP + 1), previous = null))
    }

    @Test
    fun `alternating answers on a boundary do not flicker the name`() {
        var level = Level.of(Level.MEDIUM_TOP)
        var shown = Difficulty.MEDIUM
        repeat(12) { i ->
            level = LevelLadder.next(level, if (i % 2 == 0) Outcome.CORRECT else Outcome.LOST, Pace.STEADY)
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
