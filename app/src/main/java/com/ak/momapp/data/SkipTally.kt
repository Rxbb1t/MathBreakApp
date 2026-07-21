package com.ak.momapp.data

import com.ak.momapp.problem.LevelLadder

/**
 * Counts skips IN A ROW, so that passing on the odd problem is free and
 * only a run of them says anything.
 *
 * Skipping one problem should cost nothing. The doorbell goes, she does
 * not fancy this one, she wants to see what else there is: none of that is
 * evidence that the level is wrong, and an app that docks you for every
 * single pass is one you start to feel watched by.
 *
 * A run is different. Three in a row usually does mean the problems have
 * got ahead of her, and six in a row says it louder, so the price goes up
 * and stays up. Answering anything at all breaks the run.
 *
 * Pulled out of [ProgressRepository] so the rule can be tested without an
 * Android context behind it.
 */
object SkipTally {

    /**
     * Files one skip against the running [streak] of them.
     *
     * Returns the streak to store, and what this skip should cost. Zero
     * means it costs nothing, which is the usual answer.
     */
    fun record(streak: Int): Pair<Int, Int> {
        val next = streak + 1
        val penalty = when {
            next % LevelLadder.SKIPS_BEFORE_PENALTY != 0 -> 0
            next >= LevelLadder.SKIPS_BEFORE_BIGGER_PENALTY -> LevelLadder.STEP_SKIP_PERSISTENT
            else -> LevelLadder.STEP_SKIP
        }
        return next to penalty
    }
}
