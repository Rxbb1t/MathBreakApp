package com.ak.momapp.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Simulates every palette in both modes and checks WCAG contrast, so a
 * palette tweak can't quietly produce unreadable text anywhere.
 */
class PaletteContrastTest {

    /** WCAG 2 contrast ratio, 1..21. */
    private fun contrast(a: Color, b: Color): Float {
        val la = a.luminance() + 0.05f
        val lb = b.luminance() + 0.05f
        return maxOf(la, lb) / minOf(la, lb)
    }

    private fun check(what: String, fg: Color, bg: Color, minimum: Float) {
        val ratio = contrast(fg, bg)
        assertTrue("$what contrast $ratio < $minimum", ratio >= minimum)
    }

    private fun checkScheme(name: String, scheme: ColorScheme) {
        // The soft settings/problem cards: 40% surfaceVariant over the background.
        val card = scheme.surfaceVariant.copy(alpha = 0.4f).compositeOver(scheme.background)

        // Body text (normal size) needs 4.5:1; big/bold UI labels need 3:1.
        check("$name onBackground/background", scheme.onBackground, scheme.background, 4.5f)
        check("$name onBackground/card", scheme.onBackground, card, 4.5f)
        check("$name onSurfaceVariant/background", scheme.onSurfaceVariant, scheme.background, 4.5f)
        check("$name onSurfaceVariant/card", scheme.onSurfaceVariant, card, 4.5f)
        check("$name title primary/card", scheme.primary, card, 3f)
        check("$name onPrimary/primary", scheme.onPrimary, scheme.primary, 4.5f)
        check("$name onPrimaryContainer/primaryContainer", scheme.onPrimaryContainer, scheme.primaryContainer, 4.5f)
        check("$name onSecondaryContainer/secondaryContainer", scheme.onSecondaryContainer, scheme.secondaryContainer, 4.5f)
        check("$name onTertiaryContainer/tertiaryContainer", scheme.onTertiaryContainer, scheme.tertiaryContainer, 4.5f)
        check("$name secondary/background", scheme.secondary, scheme.background, 3f)
        check("$name tertiary/background", scheme.tertiary, scheme.background, 3f)
    }

    /**
     * Both skins, because Modern replaces background, surface and the
     * container ladder while keeping the same on-colours. That is exactly
     * the kind of change that can quietly cost contrast, so it gets the
     * same sweep Legacy has always had.
     */
    @Test
    fun `every palette stays readable in light and dark mode`() {
        for (palette in AppPalette.entries) {
            for (skin in UiSkin.entries) {
                checkScheme("$palette-light-$skin", palette.colors(darkTheme = false, skin = skin))
                checkScheme("$palette-dark-$skin", palette.colors(darkTheme = true, skin = skin))
            }
        }
    }

    /**
     * Modern stacks surfaces on top of each other to build depth, so the
     * text sitting on the highest container has to survive it too. Legacy
     * never uses these roles.
     */
    @Test
    fun `modern container roles stay readable`() {
        for (palette in AppPalette.entries) {
            for (dark in listOf(false, true)) {
                val scheme = palette.colors(darkTheme = dark, skin = UiSkin.MODERN)
                val name = "$palette-${if (dark) "dark" else "light"}"
                listOf(
                    "Lowest" to scheme.surfaceContainerLowest,
                    "Low" to scheme.surfaceContainerLow,
                    "Container" to scheme.surfaceContainer,
                    "High" to scheme.surfaceContainerHigh,
                    "Highest" to scheme.surfaceContainerHighest,
                ).forEach { (role, container) ->
                    check("$name onSurface/$role", scheme.onSurface, container, 4.5f)
                    check("$name onSurfaceVariant/$role", scheme.onSurfaceVariant, container, 4.5f)
                }
            }
        }
    }

    @Test
    fun `modern defines the container ladder from the palette seed`() {
        AppPalette.entries.forEach { palette ->
            listOf(true, false).forEach { dark ->
                val modern = palette.colors(dark, UiSkin.MODERN)
                val legacy = palette.colors(dark, UiSkin.LEGACY)
                // The five container roles must differ from each other, or
                // there is no ladder to build depth on.
                val ladder = listOf(
                    modern.surfaceContainerLowest, modern.surfaceContainerLow,
                    modern.surfaceContainer, modern.surfaceContainerHigh,
                    modern.surfaceContainerHighest,
                )
                assertEquals("$palette $dark ladder has duplicates", 5, ladder.toSet().size)
                // Legacy must keep Material's baseline outlineVariant, which
                // is the bug the Stats chart shows today.
                assertNotEquals(
                    "$palette $dark: modern must not inherit the baseline outline",
                    legacy.outlineVariant, modern.outlineVariant,
                )
            }
        }
    }

    @Test
    fun `always-dark palettes render dark even in light mode`() {
        val alwaysDark = AppPalette.entries.filter { it.alwaysDark }
        assertTrue(
            "expected Forest and Midnight to be always-dark",
            alwaysDark.containsAll(listOf(AppPalette.FOREST, AppPalette.MIDNIGHT)),
        )
        for (palette in alwaysDark) {
            val light = palette.colors(darkTheme = false)
            assertTrue(
                "$palette light-mode background is not dark",
                light.background.luminance() < 0.1f,
            )
        }
    }
}
