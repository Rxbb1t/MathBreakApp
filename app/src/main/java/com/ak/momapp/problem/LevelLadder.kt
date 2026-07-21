package com.ak.momapp.problem

import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * How quickly an answer arrived, judged against how long she usually takes
 * on that kind of problem rather than against a stopwatch figure someone
 * picked in advance.
 */
enum class Pace {
    /** Well under her usual time: the problem was not making her think. */
    FAST,

    /** About her usual time: the level is about right. */
    STEADY,

    /** Well over her usual time: right, but it cost her. */
    SLOW,
}

/**
 * What she usually takes on one topic, as a rolling average.
 *
 * The app used to collect solve time and throw it away, keeping only the
 * single fastest answer for the stats screen. But a right answer in four
 * seconds and a right answer in four minutes say opposite things about
 * whether the level fits, and the ladder could not tell them apart.
 *
 * There is no fixed idea of a "normal" number of seconds here, because
 * there isn't one: it depends on the person, the topic, and whether she is
 * reading a story problem or tapping a sign. So the app learns her pace
 * per topic and compares her against herself.
 */
data class PaceEstimate(
    /** Rolling average of her first-try solve times, in millis. */
    val typicalMs: Long = 0,
    /** Timed answers behind [typicalMs]. */
    val samples: Int = 0,
) {

    /**
     * Everything is [Pace.STEADY] until there is enough evidence to have an
     * opinion. Guessing at her pace from one or two answers would move the
     * level on noise, which is the thing this whole change is trying to
     * stop doing.
     */
    fun classify(solveTimeMs: Long): Pace = when {
        solveTimeMs <= 0 || samples < EVIDENCE_NEEDED -> Pace.STEADY
        solveTimeMs < typicalMs * FAST_RATIO -> Pace.FAST
        solveTimeMs > typicalMs * SLOW_RATIO -> Pace.SLOW
        else -> Pace.STEADY
    }

    /**
     * Folds one solve time into the average.
     *
     * Answers longer than [SANE_MAX_MS] are classified but never learned
     * from: a problem left on screen while she answers the door is not
     * evidence about her pace, and letting one in would drag the average up
     * for days and make everything afterwards look fast.
     */
    fun record(solveTimeMs: Long): PaceEstimate = when {
        solveTimeMs <= 0 || solveTimeMs > SANE_MAX_MS -> this
        samples == 0 -> PaceEstimate(solveTimeMs, 1)
        else -> PaceEstimate(
            typicalMs = (typicalMs * (1 - ALPHA) + solveTimeMs * ALPHA).roundToLong(),
            samples = samples + 1,
        )
    }

    companion object {
        /** Timed answers before her pace is trusted enough to act on. */
        const val EVIDENCE_NEEDED = 5

        /** Under this share of her usual time counts as fast. */
        const val FAST_RATIO = 0.6

        /** Over this multiple of her usual time counts as slow. */
        const val SLOW_RATIO = 1.7

        /** Weight of the newest answer in the rolling average. */
        const val ALPHA = 0.3

        /** Longer than this and she was not looking at the screen. */
        const val SANE_MAX_MS = 4 * 60 * 1000L
    }
}

/**
 * What actually happened to a problem, as far as the ladder is concerned.
 *
 * These used to collapse into one boolean, which quietly made walking away
 * from a problem mean the same thing as getting it wrong. They do not mean
 * the same thing: a wrong answer is evidence about the level, whereas a
 * skip is usually evidence about the moment. She is busy, the kitchen is
 * on fire, she does not fancy this one.
 */
enum class Outcome {
    CORRECT,

    /**
     * A wrong attempt on a problem she can still get. COSTS NOTHING.
     *
     * Trying something and finding out it was wrong is how the exercise
     * works, and charging for it makes the safe move not answering. What
     * says the level is too high is failing to get there at all, not the
     * stumble on the way.
     */
    WRONG,

    /** Out of tries: the problem is gone and the answer is on screen. */
    LOST,

    /** Walked away from it. Costs something, but only every third time. */
    SKIPPED,

    /** The countdown ran out, or she asked to be shown the answer. */
    GAVE_UP,
}

/**
 * Moves a [Level] by a few points per answer.
 *
 * The old ladder counted streaks and jumped a whole third of the range
 * when one filled up. This one nudges: a couple of dozen steady right
 * answers to cross a band, fewer if they come quickly, and a wrong answer
 * costs several right ones. She can drift up through Easy for a week
 * before the word "Normal" ever appears, and the problems will have been
 * getting gently harder the whole time.
 *
 * How far one answer carries her also depends on WHAT it was: see
 * [Problem.effort]. Tapping ✓ or ✗ is not the same achievement as solving
 * a two-unknown equation, and the ladder should not pretend otherwise.
 */
object LevelLadder {

    /**
     * THE NUMBER THAT DECIDES EVERYTHING, and the one worth understanding
     * before touching any of the steps below.
     *
     * A points ladder stops moving where the climbing and the falling
     * cancel out, so `p × STEP_STEADY = (1 − p) × STEP_LOST` fixes the
     * share it hunts for: whatever she does, it drags her toward the level
     * where she SOLVES that many. The gain steps and [STEP_LOST] are not
     * independent tuning knobs. Their ratio names a target.
     *
     * Note what p counts now: problems SOLVED, not first attempts right.
     * Since a wrong attempt costs nothing and only losing a problem does,
     * two stumbles ending in the right answer read as a success here. The
     * first-try accuracy she settles at is therefore lower than this
     * number, which is intended: it leaves room to be wrong on the way.
     *
     * At [STEP_STEADY] 2.4 against [STEP_LOST] 5 the target is about 68%
     * SOLVED. Lower than the old 80% on purpose: the user found that too
     * punishing, so the gains were raised and the loss softened, both of
     * which let the level sit a little higher, where she is stretched more
     * often. The flip side is that she loses a slightly larger share of
     * problems outright; that is the deliberate trade, and [STEP_LOST] is
     * the lever if it ever needs pulling back.
     */
    const val TARGET_ACCURACY = 0.68

    /**
     * The three climb sizes. Twenty percent above the old 3 / 2 / 1: the
     * user wanted a right answer, and a quick one especially, to feel like
     * it moved the needle more. Doubles because the boost only survives
     * rounding on some of them otherwise.
     */
    const val STEP_FAST = 3.6

    /** Right, at her usual pace: the level fits, so inch upward. */
    const val STEP_STEADY = 2.4

    /** Right, but it took her: she is about where she should be. */
    const val STEP_SLOW = 1.2

    /**
     * LOSING a problem: out of tries, answer revealed, or the clock ran
     * out. A flat five points whatever the problem was worth.
     *
     * This is the only thing that costs anything besides a run of skips,
     * and it is charged once at the end, not per wrong attempt. Two
     * stumbles followed by the right answer is a solved problem and is paid
     * for as one.
     */
    const val STEP_LOST = 5

    /** Giving up, or the clock running out: the same flat five as a loss. */
    const val STEP_GAVE_UP = 5

    /**
     * What one skip costs. Flat, and charged every time.
     *
     * It used to be free for the first two of a run and then escalate; the
     * user preferred a plain, predictable three points. Small enough that
     * the odd pass barely registers, real enough that skipping everything
     * still walks the level down.
     */
    const val STEP_SKIP = 3

    /**
     * The most a single answer can move her, which is what [BAND_HYSTERESIS]
     * has to clear. A loss is capped at the flat step even for the heaviest
     * problems.
     */
    const val MAX_DROP = STEP_LOST

    /**
     * How far past a boundary the points must travel before the NAME
     * changes.
     *
     * Without this the label flickers: sitting on 66 and answering one
     * right, one wrong reads as "Normal, Hard, Normal, Hard" on the
     * Exercises screen, which looks broken and invites her to chase it. The
     * points underneath still move freely. Only the word is sticky.
     *
     * It must stay LARGER THAN [MAX_DROP], and that is not a coincidence
     * to be tuned away: it is what keeps the standing rule that a single
     * wrong answer never lowers the level she is shown. One miss cannot
     * carry the points a full margin past a boundary it started on the
     * right side of, so the name survives it. Two in a row can.
     */
    const val BAND_HYSTERESIS = MAX_DROP + 1

    /**
     * Where one answer leaves her.
     *
     * [effort] is how much this particular problem is worth ([Problem.effort]).
     * It multiplies the climb outright, so the demanding kinds carry her
     * further, but it is capped at 1 on the way DOWN: a hard problem should
     * be worth more when solved without also costing more when missed,
     * or the exercises worth reaching for would be the ones punishing her.
     */
    fun next(
        level: Level,
        outcome: Outcome,
        pace: Pace,
        effort: Double = 1.0,
        ceiling: Level = Level.CEILING,
    ): Level {
        val step = when (outcome) {
            Outcome.CORRECT -> {
                val base = when (pace) {
                    Pace.FAST -> STEP_FAST
                    Pace.SLOW -> STEP_SLOW
                    Pace.STEADY -> STEP_STEADY
                }
                // At least one point, so a right answer always registers.
                maxOf(1, (base * effort).roundToInt())
            }
            // A stumble on the way to the right answer is free.
            Outcome.WRONG -> 0
            Outcome.LOST -> -(STEP_LOST * minOf(effort, 1.0)).roundToInt()
            Outcome.GAVE_UP -> -STEP_GAVE_UP
            Outcome.SKIPPED -> -STEP_SKIP
        }
        val moved = level.shift(step)
        // The ceiling holds a climb back, but never shoves her down: a cap
        // applied after she has already climbed past it should stop her
        // going further, not snatch away ground she already has.
        return if (step > 0) minOf(moved, maxOf(ceiling, level)) else moved
    }

    /**
     * The band to actually show, given the one she was shown last time.
     * Changes only once the points are [BAND_HYSTERESIS] clear of the
     * boundary, so a level hovering on an edge keeps its name.
     */
    fun shownBand(level: Level, previous: Difficulty?): Difficulty {
        val raw = level.band
        if (previous == null || raw == previous) return raw
        return if (raw > previous) {
            if (level.points >= bandStart(raw) + BAND_HYSTERESIS) raw else previous
        } else {
            if (level.points <= bandEnd(raw) - BAND_HYSTERESIS) raw else previous
        }
    }

    private fun bandStart(band: Difficulty): Int = when (band) {
        Difficulty.EASY -> Level.MIN
        Difficulty.MEDIUM -> Level.EASY_TOP + 1
        Difficulty.HARD -> Level.MEDIUM_TOP + 1
    }

    private fun bandEnd(band: Difficulty): Int = when (band) {
        Difficulty.EASY -> Level.EASY_TOP
        Difficulty.MEDIUM -> Level.MEDIUM_TOP
        Difficulty.HARD -> Level.MAX
    }
}
