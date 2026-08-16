package com.ak.momapp.ui.problem

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A stray second tap must not answer the problem that just arrived.
 * TRUE_FALSE has one attempt, so an accidental double-tap loses the whole
 * problem and costs seven points.
 */
class InputLockoutTest {

    @Test
    fun `a tap arriving with the problem is ignored`() {
        assertFalse(acceptsInputAt(dealtAt = 1_000L, now = 1_000L))
        assertFalse(acceptsInputAt(dealtAt = 1_000L, now = 1_100L))
    }

    @Test
    fun `a tap after the lockout is accepted`() {
        val after = 1_000L + ProblemViewModel.INPUT_LOCKOUT_MS
        assertTrue(acceptsInputAt(dealtAt = 1_000L, now = after))
        assertTrue(acceptsInputAt(dealtAt = 1_000L, now = after + 5_000L))
    }

    @Test
    fun `the lockout is short enough not to be felt`() {
        assertTrue(ProblemViewModel.INPUT_LOCKOUT_MS in 200L..500L)
    }

    /**
     * The clock this reads is wall time, which an OS or a time-zone change
     * can move backwards. A jump must not leave her unable to answer, so
     * anything not clearly inside the window is allowed through.
     */
    @Test
    fun `a clock that jumps backwards does not lock her out`() {
        assertTrue(acceptsInputAt(dealtAt = 10_000L, now = 1_000L))
    }
}
