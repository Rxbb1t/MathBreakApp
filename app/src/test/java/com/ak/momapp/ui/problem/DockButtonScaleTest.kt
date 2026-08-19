package com.ak.momapp.ui.problem

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The dock's two quieter buttons were deliberately taken down a size, and
 * the daily challenge reads the same two numbers so its Check and Hint
 * cannot drift from the break screen's. Pinned here because "15% smaller"
 * is a decision, not a detail, and nothing else on screen would notice if
 * one of the constants moved.
 */
class DockButtonScaleTest {

    @Test
    fun `hint and skip are fifteen percent down, check ten`() {
        assertEquals(0.85f, QuietButtonScale, 0.0001f)
        assertEquals(0.90f, CheckButtonScale, 0.0001f)
    }

    @Test
    fun `scaling takes both dimensions with it`() {
        val scaled = TextStyle(fontSize = 20.sp, lineHeight = 30.sp).scaledBy(0.85f)
        assertEquals(17.sp, scaled.fontSize)
        assertEquals(25.5.sp, scaled.lineHeight)
    }

    /**
     * Legacy's typography is the Material baseline, and a Material style
     * can leave either dimension unspecified. Multiplying one throws, so
     * the helper has to step over it rather than scale it -- and a Legacy
     * button that crashed on being drawn would be a poor way to find out.
     */
    @Test
    fun `an unspecified size survives instead of throwing`() {
        val scaled = TextStyle(
            fontSize = TextUnit.Unspecified,
            lineHeight = TextUnit.Unspecified,
        ).scaledBy(0.85f)
        assertEquals(TextUnit.Unspecified, scaled.fontSize)
        assertEquals(TextUnit.Unspecified, scaled.lineHeight)
    }
}
