package com.ak.momapp.problem

import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * How hard a problem should be, on a fine 0–100 scale.
 *
 * [Difficulty] used to be the whole story: three values that decided both
 * which generators could run and how big their numbers were. Three steps
 * is too coarse to follow someone. Getting three answers right moved her a
 * full third of the range at once, so the app was always either too easy
 * or suddenly too hard, with nothing in between.
 *
 * So the real signal is these points, and Easy/Normal/Hard survive as
 * BANDS over them. She still only ever sees the three names, which is the
 * point: the vocabulary she has stays true while the thing underneath it
 * moves in small steps.
 *
 * Generators read the points, not the band, wherever the size of the
 * numbers is what makes a problem hard. Use [between] to turn the old
 * three-value tuning tables into a smooth curve, and [ramp] to fade a
 * whole shape of problem in or out across a stretch of the scale.
 */
@JvmInline
value class Level private constructor(val points: Int) : Comparable<Level> {

    /** The name she sees. Bands are equal thirds of the scale. */
    val band: Difficulty
        get() = when {
            points <= EASY_TOP -> Difficulty.EASY
            points <= MEDIUM_TOP -> Difficulty.MEDIUM
            else -> Difficulty.HARD
        }

    /** Position across the whole scale, 0.0 at the floor and 1.0 at the top. */
    val fraction: Double get() = points / MAX.toDouble()

    /** A level [n] points along, clamped to the scale. */
    fun shift(n: Int): Level = of(points + n)

    override fun compareTo(other: Level): Int = points.compareTo(other.points)

    /**
     * Interpolates between three tuned values pinned at the middle of each
     * band, holding flat beyond the outer two.
     *
     * This is what lets the generators become continuous without throwing
     * away years of tuning: an old `when (difficulty)` table of three
     * numbers becomes `level.between(a, b, c)` and still produces exactly
     * a, b and c at the middle of Easy, Normal and Hard. What is new is
     * everything in between.
     */
    fun between(easy: Int, medium: Int, hard: Int): Int =
        between(easy.toDouble(), medium.toDouble(), hard.toDouble()).roundToInt()

    fun between(easy: Double, medium: Double, hard: Double): Double = when {
        points <= EASY_ANCHOR -> easy
        points <= MEDIUM_ANCHOR ->
            lerp(easy, medium, (points - EASY_ANCHOR).toDouble() / (MEDIUM_ANCHOR - EASY_ANCHOR))
        points <= HARD_ANCHOR ->
            lerp(medium, hard, (points - MEDIUM_ANCHOR).toDouble() / (HARD_ANCHOR - MEDIUM_ANCHOR))
        else -> hard
    }

    /** The same three-anchor curve applied to both ends of a range. */
    fun span(easy: IntRange, medium: IntRange, hard: IntRange): IntRange {
        val lo = between(easy.first, medium.first, hard.first)
        val hi = between(easy.last, medium.last, hard.last)
        return lo..maxOf(lo, hi)
    }

    /**
     * How far into a window this level sits: 0.0 at or below [from], 1.0 at
     * or above [to], sliding linearly between.
     *
     * This is how a kind of problem arrives or leaves. Rather than geometry
     * being flatly absent at Easy and abruptly present at Normal, its share
     * of the mix ramps up across the stretch where it starts to make sense.
     */
    fun ramp(from: Int, to: Int): Double = when {
        points <= from -> 0.0
        points >= to -> 1.0
        else -> (points - from).toDouble() / (to - from)
    }

    /** The reverse ramp: full below [from], gone above [to]. */
    fun fade(from: Int, to: Int): Double = 1.0 - ramp(from, to)

    companion object {
        const val MIN = 0
        const val MAX = 100

        /** Band edges. Equal thirds, so each name covers the same width. */
        const val EASY_TOP = 32
        const val MEDIUM_TOP = 66

        /**
         * The middle of each band. A [Difficulty] converts to the middle of
         * its own band, and [between]'s tuned values are pinned here.
         */
        const val EASY_ANCHOR = 16
        const val MEDIUM_ANCHOR = 50
        const val HARD_ANCHOR = 83

        val FLOOR = Level(MIN)
        val CEILING = Level(MAX)

        fun of(points: Int): Level = Level(points.coerceIn(MIN, MAX))

        /** The middle of a band, which is where a named level sits. */
        fun of(band: Difficulty): Level = when (band) {
            Difficulty.EASY -> Level(EASY_ANCHOR)
            Difficulty.MEDIUM -> Level(MEDIUM_ANCHOR)
            Difficulty.HARD -> Level(HARD_ANCHOR)
        }

        private fun lerp(from: Double, to: Double, t: Double): Double =
            from + (to - from) * t.coerceIn(0.0, 1.0)
    }
}

/** The middle of this band, as a point on the fine scale. */
fun Difficulty.toLevel(): Level = Level.of(this)

/**
 * Picks one option, each carrying a non-negative weight.
 *
 * Worth having rather than nesting `if (random < ramp)` branches, because
 * a bare ramp reaches 1.0 at the top of its window and a branch guarded by
 * one therefore becomes CERTAIN there, silently removing everything below
 * it. Written as weights, "this shape arrives" and "that shape stays"
 * compose the obvious way and nothing can quietly swallow the rest.
 */
fun <T> Random.pickWeighted(options: List<Pair<T, Double>>): T {
    val total = options.sumOf { it.second }
    if (total <= 0.0) return options.first().first
    var roll = nextDouble() * total
    for ((value, weight) in options) {
        roll -= weight
        if (roll < 0) return value
    }
    return options.last().first
}
