package com.ak.momapp.data

import com.ak.momapp.problem.LevelLadder

/**
 * Counts skips so that only every so often costs anything.
 *
 * Passing on one problem should be free. The doorbell goes, she does not
 * fancy this one, she wants to see what else there is: none of that is
 * evidence that the level is wrong, and an app that docks you for every
 * single pass is one you start to feel watched by.
 *
 * Two skips in a row is a different signal. That one usually does mean the
 * problems have got ahead of her, and it is worth hearing.
 *
 * Pulled out of [ProgressRepository] so the rule can be tested without an
 * Android context behind it.
 */
object SkipTally {

    /**
     * Files one skip against the running [tally].
     *
     * Returns the tally to store and whether this skip should actually move
     * the level.
     */
    fun record(tally: Int): Pair<Int, Boolean> {
        val next = tally + 1
        return if (next >= LevelLadder.SKIPS_PER_PENALTY) 0 to true else next to false
    }
}
