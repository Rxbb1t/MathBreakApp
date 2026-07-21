package com.ak.momapp.problem

/**
 * Eases the first couple of problems of a break.
 *
 * Sitting down cold to a problem right at your level is a small jolt; a
 * gentler one or two to start with lets her find her feet. Nothing about
 * this is shown or named. The problems are simply a little easier at the
 * top of a sitting and back to normal by the third, and only her sense of
 * "oh, I've got this" gives it away.
 *
 * It touches only what is DEALT, never what is stored: her real level and
 * everything the ladders do with an answer are untouched, so a warm-up
 * problem still counts for exactly what it is worth. She is being handed
 * an easier problem, not being secretly marked down.
 */
object Warmup {

    /** How many problems at the start of a sitting are eased. */
    const val PROBLEMS = 2

    /** How far the very first problem drops, in points; it tapers to zero. */
    const val DROP = 18

    /**
     * The level to actually deal [base] at, given how many problems she has
     * already finished this sitting. The first is eased most, the second
     * half as much, the third onward not at all.
     */
    fun ease(base: Level, problemsDone: Int): Level {
        if (problemsDone >= PROBLEMS) return base
        val offset = DROP * (PROBLEMS - problemsDone) / PROBLEMS
        return base.shift(-offset)
    }
}
