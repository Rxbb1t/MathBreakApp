package com.ak.momapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Legacy is the look the app shipped through v1.6, and its whole value is
 * that it does not move. These assertions are the record of what it was;
 * if one fails, either the change belongs in Modern or the record needs a
 * deliberate update.
 */
class LegacyFrozenTest {

    @Test
    fun `legacy body text is unchanged`() {
        val body = LegacyTypography.bodyLarge
        assertEquals(FontFamily.Default, body.fontFamily)
        assertEquals(18.sp, body.fontSize)
        assertEquals(26.sp, body.lineHeight)
        assertEquals(0.5.sp, body.letterSpacing)
    }

    /**
     * Everything except bodyLarge is left at the Material baseline, so it
     * is compared against a fresh default rather than against a copied
     * list of values that would rot. Note the baseline is
     * FontFamily.SansSerif, not null: Material fills the family in.
     */
    @Test
    fun `legacy customises nothing else`() {
        val baseline = Typography()
        assertEquals(baseline.titleLarge, LegacyTypography.titleLarge)
        assertEquals(baseline.labelLarge, LegacyTypography.labelLarge)
        assertEquals(baseline.displayLarge, LegacyTypography.displayLarge)
        assertEquals(baseline.headlineSmall, LegacyTypography.headlineSmall)
    }

    @Test
    fun `modern uses inter everywhere it sets a family`() {
        listOf(
            ModernTypography.bodyLarge,
            ModernTypography.titleLarge,
            ModernTypography.labelLarge,
            ModernTypography.headlineSmall,
            ModernTypography.displayLarge,
        ).forEach { assertEquals(InterFamily, it.fontFamily) }
    }

    /**
     * The spec calls tabular figures functional rather than decorative:
     * without them the answer field reflows under her thumb as she types.
     * Easy to lose in a later edit, so it is pinned here.
     */
    @Test
    fun `modern sets tabular figures on every style`() {
        listOf(
            ModernTypography.displayLarge,
            ModernTypography.headlineSmall,
            ModernTypography.titleLarge,
            ModernTypography.titleMedium,
            ModernTypography.bodyLarge,
            ModernTypography.bodyMedium,
            ModernTypography.labelLarge,
            ModernTypography.labelSmall,
        ).forEach { assertEquals("tnum", it.fontFeatureSettings) }
    }

    @Test
    fun `legacy and modern text scales differ by a quarter`() {
        assertEquals(0.99f, UiSkin.LEGACY.textScale, 0.0001f)
        assertEquals(0.75f, UiSkin.MODERN.textScale, 0.0001f)
    }

    @Test
    fun `each skin hands back its own typography`() {
        assertEquals(LegacyTypography, UiSkin.LEGACY.typography)
        assertEquals(ModernTypography, UiSkin.MODERN.typography)
    }
}
