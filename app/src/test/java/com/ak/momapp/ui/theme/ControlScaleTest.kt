package com.ak.momapp.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Text and touch targets are different problems. Type follows her setting
 * without limit; a control must not shrink because the type did, and must
 * not grow without bound because the type did.
 */
class ControlScaleTest {

    @Test
    fun `a control never shrinks below its base size`() {
        assertEquals(1.0f, controlScaleFor(0.5f), 0.0001f)
        assertEquals(1.0f, controlScaleFor(0.85f), 0.0001f)
        assertEquals(1.0f, controlScaleFor(1.0f), 0.0001f)
    }

    @Test
    fun `a control grows with the accessibility setting`() {
        assertEquals(1.2f, controlScaleFor(1.2f), 0.0001f)
    }

    @Test
    fun `a control stops growing at the ceiling`() {
        assertEquals(1.45f, controlScaleFor(1.45f), 0.0001f)
        assertEquals(1.45f, controlScaleFor(2.0f), 0.0001f)
        assertEquals(1.45f, controlScaleFor(3.0f), 0.0001f)
    }

    @Test
    fun `the modern skin is not capped for chrome and legacy is`() {
        assertEquals(1.5f, UiSkin.LEGACY.chromeFontScaleCeiling, 0.0001f)
        assertEquals(2.4f, minOf(2.4f, UiSkin.MODERN.chromeFontScaleCeiling), 0.0001f)
    }

    /**
     * The shrink floor is deliberately NOT the chrome ceiling, even though
     * both happen to be 1.5. Modern's chrome has no ceiling, and reading
     * one from the other would leave long problem text unable to shrink
     * onto one screen at a large font size. See ProblemTextCard.
     */
    @Test
    fun `the shrink floor keeps its own ceiling for both skins`() {
        assertEquals(1.5f, ProblemFloorScaleCeiling, 0.0001f)
    }
}
