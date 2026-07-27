package com.ak.momapp.data

import com.ak.momapp.problem.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The three presets have to read as ONE DIAL: relaxed, then default, then
 * challenge, with every axis moving the same way.
 *
 * Worth pinning because it broke silently once. The countdown used to be
 * most of what separated them, and removing it left DEFAULT uncapped while
 * CHALLENGE stopped at ten problems, so the demanding option was quietly
 * the shorter one. Nothing failed; the presets simply stopped meaning what
 * their names said.
 */
class SetupPresetTest {

    /** Relaxed to challenging. Every assertion below reads in this order. */
    private val ordered = listOf(SetupPreset.RELAXED, SetupPreset.DEFAULT, SetupPreset.CHALLENGE)

    @Test
    fun `every preset is covered by the ordering`() {
        assertEquals(SetupPreset.entries.toSet(), ordered.toSet())
    }

    @Test
    fun `breaks get longer, never shorter`() {
        val lengths = ordered.map { it.problemsPerBreak }
        assertEquals("$lengths should be ascending", lengths.sorted(), lengths)
        // An unlimited preset would read as zero here and silently invert
        // the whole ordering, which is exactly the bug this file exists for.
        assertTrue(
            "a preset with no cap cannot be placed on this dial: $lengths",
            lengths.none { it == BrainBreakSettings.UNLIMITED_PROBLEMS },
        )
    }

    @Test
    fun `starting level never goes down the list`() {
        val starts = ordered.map { it.startingDifficulty }
        assertEquals("$starts should be ascending", starts.sorted(), starts)
    }

    @Test
    fun `the ceiling never goes down the list`() {
        val ceilings = ordered.map { it.maxDifficulty }
        assertEquals("$ceilings should be ascending", ceilings.sorted(), ceilings)
    }

    @Test
    fun `a preset never starts above its own ceiling`() {
        for (preset in SetupPreset.entries) {
            assertTrue(
                "$preset starts at ${preset.startingDifficulty} but is capped at ${preset.maxDifficulty}",
                preset.startingDifficulty <= preset.maxDifficulty,
            )
        }
    }

    /**
     * Only RELAXED holds the level back. If a second preset ever capped the
     * climb the Exercises screen would show two of them stuck at Normal for
     * reasons she was never told about.
     */
    @Test
    fun `relaxed is the only one that caps the climb`() {
        val capped = SetupPreset.entries.filter { it.maxDifficulty != Difficulty.HARD }
        assertEquals(listOf(SetupPreset.RELAXED), capped)
    }

    /**
     * The presets should hand her values she could also have picked herself,
     * or the Settings chips will show nothing selected right after she
     * applies one.
     */
    @Test
    fun `every preset length is one of the settings chips`() {
        for (preset in SetupPreset.entries) {
            assertTrue(
                "${preset.problemsPerBreak} is not offered by the per-break chips",
                preset.problemsPerBreak in BrainBreakSettings.PROBLEM_LIMIT_PRESETS,
            )
        }
    }
}
