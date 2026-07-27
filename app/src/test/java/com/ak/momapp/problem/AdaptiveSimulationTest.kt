package com.ak.momapp.problem

import kotlin.math.abs
import kotlin.random.Random
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Does the ladder actually FIND someone's level, and then hold it?
 *
 * Believing an adaptive system helps is easy and usually wrong, so this
 * measures it instead. A simulated player has a fixed true ability on the
 * same 0–100 scale, and answers each problem correctly with a probability
 * that falls as the problem outruns her: comfortably below her level she
 * almost always gets it, far above it she almost never does.
 *
 * That model is a simplification and does not need to be exact. What it
 * catches is the failures that matter and are invisible by inspection: a
 * ladder that never reaches a high ability, one that overshoots and parks
 * somebody at the ceiling, or one that oscillates so wildly that the level
 * she sees is essentially noise.
 */
class AdaptiveSimulationTest {

    /**
     * Chance of a correct answer when someone of [ability] meets a problem
     * at [level]. A gentle curve rather than a cliff: at her own level she
     * is right about four times in five, which is roughly where practice
     * should sit.
     */
    private fun chanceCorrect(ability: Int, level: Level): Double {
        val gap = (ability - level.points) / 18.0
        return (0.5 + 0.4 * gap).coerceIn(0.03, 0.97)
    }

    /**
     * Answering time follows the same gap: work well within her range comes
     * quickly, work at the edge of it takes a while. This is what the pace
     * signal reads, so the simulation has to produce it honestly rather
     * than handing the ladder the answer.
     */
    private fun solveTimeMs(ability: Int, level: Level, random: Random): Long {
        val stretch = ((level.points - ability) / 25.0).coerceIn(-1.0, 2.0)
        val base = 20_000 * (1.0 + stretch)
        return (base * (0.7 + random.nextDouble() * 0.6)).toLong().coerceAtLeast(1_000L)
    }

    /**
     * Runs one player and returns the level after each answer.
     *
     * The run of clean answers is carried here rather than ignored, because
     * the bonus it pays is part of what decides where the ladder settles.
     * Simulating without it would measure a ladder the app does not have.
     */
    private fun run(ability: Int, problems: Int, start: Level, seed: Int): List<Level> {
        val random = Random(seed)
        var level = start
        var pace = PaceEstimate()
        var streak = 0
        return List(problems) {
            val correct = random.nextDouble() < chanceCorrect(ability, level)
            val time = solveTimeMs(ability, level, random)
            val judged = pace.classify(time)
            if (correct) pace = pace.record(time)
            val outcome = if (correct) Outcome.CORRECT else Outcome.LOST
            streak = LevelLadder.streakAfter(streak, outcome)
            level = LevelLadder.next(level, outcome, judged, streak = streak)
            level
        }
    }

    /** Where the ladder settled: the middle of the last stretch of answers. */
    private fun settledAt(history: List<Level>, tail: Int = 40): Double =
        history.takeLast(tail).map { it.points }.average()

    /**
     * THE ONE THAT MATTERS. Not "does it find her ability" but "does it
     * park her somewhere pleasant to be": the level where she gets about
     * [LevelLadder.TARGET_ACCURACY] of them right and is stretched by the
     * rest.
     *
     * Asserting proximity to the simulated ability instead would be a
     * weaker and vaguer claim, and it is what let an earlier draft look
     * fine while hunting for a level where she got nearly half wrong.
     */
    @Test
    fun `it parks her where she gets most of them right`() {
        for (ability in listOf(15, 35, 50, 70, 90)) {
            val settled = settledAt(run(ability, problems = 300, start = Level.of(50), seed = ability))
            val accuracy = chanceCorrect(ability, Level.of(settled.toInt()))
            assertTrue(
                "ability $ability settled at ${"%.1f".format(settled)}, " +
                    "where she would get ${"%.0f".format(accuracy * 100)}% right",
                accuracy > LevelLadder.TARGET_ACCURACY - 0.15,
            )
            // And still stretched: parking her somewhere she never misses
            // would be just as wrong, in the other direction.
            assertTrue(
                "ability $ability parked at a level she never misses",
                accuracy < 0.97,
            )
        }
    }

    @Test
    fun `a competent player is not rushed to the top of the scale`() {
        // Someone answering 80% right is, by definition, already where she
        // belongs. The level should drift, not bolt.
        val history = run(ability = 40, problems = 60, start = Difficulty.EASY.toLevel(), seed = 3)
        assertTrue(
            "sixty problems carried her to ${history.last().points}",
            history.last().points < 75,
        )
    }

    @Test
    fun `it finds the same place whether it starts high or low`() {
        val ability = 60
        val fromBelow = settledAt(run(ability, 220, Level.FLOOR, seed = 1))
        val fromAbove = settledAt(run(ability, 220, Level.CEILING, seed = 2))
        assertTrue(
            "started low ${"%.1f".format(fromBelow)} vs high ${"%.1f".format(fromAbove)}",
            abs(fromBelow - fromAbove) < 15,
        )
    }

    /**
     * The number that justifies the whole change. The old ladder moved a
     * third of the range at a time, so it could not sit anywhere except
     * three fixed points; this one has to hold a genuine position without
     * wandering.
     *
     * Averaged over seeds rather than read off one. A single run's spread
     * is a noisy statistic, and an earlier version of this test asserted a
     * bound that the run it happened to use cleared by three points while
     * other seeds went past it. A threshold you only pass on the seed you
     * picked is not measuring anything.
     *
     * The bound scales with [LevelLadder.STEP_LOST] because that is what
     * sets the size of a downward move: a ladder that drops seven at a time
     * genuinely ranges wider than one that drops five, and pinning an
     * absolute number here would just have to be edited every time that
     * constant is tuned.
     */
    @Test
    fun `once settled it stays put instead of wandering`() {
        val spreads = (1..8).map { seed ->
            val history = run(ability = 55, problems = 400, start = Level.of(50), seed = seed)
            val tail = history.takeLast(200).map { it.points }
            tail.max() - tail.min()
        }
        val mean = spreads.average()
        assertTrue(
            "the level swung across ${"%.1f".format(mean)} points on average (runs: $spreads)",
            mean <= LevelLadder.STEP_LOST * 7.0,
        )
    }

    @Test
    fun `a strong player is not held down by the floor she started at`() {
        val history = run(ability = 95, problems = 250, start = Level.FLOOR, seed = 4)
        assertTrue("never reached Hard", history.last().band == Difficulty.HARD)
    }

    @Test
    fun `a struggling player is let back down`() {
        val history = run(ability = 8, problems = 250, start = Level.CEILING, seed = 5)
        assertTrue("never came back down to Easy", history.last().band == Difficulty.EASY)
    }

    /**
     * Being quick should be worth something. Two players who get the same
     * answers right, one of them fast, must not end up in the same place,
     * or the pace signal is decorative.
     */
    @Test
    fun `answering quickly carries her further than answering slowly`() {
        var fast = Level.of(40)
        var slow = Level.of(40)
        repeat(20) {
            fast = LevelLadder.next(fast, Outcome.CORRECT, Pace.FAST)
            slow = LevelLadder.next(slow, Outcome.CORRECT, Pace.SLOW)
        }
        assertTrue("fast $fast should be well past slow $slow", fast.points - slow.points > 15)
    }

    /**
     * The name she sees has to be steadier than the points underneath, or
     * the Exercises screen will look like it is flickering at her.
     */
    @Test
    fun `the name she sees changes far less often than the level does`() {
        val history = run(ability = 50, problems = 300, start = Level.of(50), seed = 12)
        var shown = Difficulty.MEDIUM
        var renames = 0
        for (level in history) {
            val next = LevelLadder.shownBand(level, shown)
            if (next != shown) renames++
            shown = next
        }
        assertTrue("the label changed $renames times in 300 problems", renames < 12)
    }
}
