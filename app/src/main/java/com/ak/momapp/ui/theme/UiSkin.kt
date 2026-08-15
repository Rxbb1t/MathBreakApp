package com.ak.momapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Which look the app wears.
 *
 * A skin changes how the app looks and never what it can do: every
 * feature ships to both, and what differs is typography, shape, colour
 * roles and elevation. Screens ask the theme for a token rather than
 * asking which skin they are; [LocalSkin] is for the few places where the
 * LAYOUT genuinely differs, and each use wants a comment saying why a
 * token could not express it.
 */
enum class UiSkin(
    /**
     * How large text renders before her system setting is applied.
     * Modern sits a quarter below Legacy, which was the explicit ask. The
     * app has no in-app text size control, so Legacy is the way back.
     */
    val textScale: Float,
    /**
     * How far chrome follows her system font size before it stops.
     * Modern's rows reflow, so it needs no ceiling; Legacy's cannot, which
     * is the only reason the cap exists.
     */
    val chromeFontScaleCeiling: Float,
) {
    /** Exactly the look shipped through v1.6. Frozen on purpose. */
    LEGACY(textScale = 0.99f, chromeFontScaleCeiling = 1.5f),

    /** Inter, tonal depth, drawn icons. */
    MODERN(textScale = 0.75f, chromeFontScaleCeiling = Float.MAX_VALUE),
    ;

    val typography: Typography
        get() = if (this == LEGACY) LegacyTypography else ModernTypography
}

val LocalSkin = staticCompositionLocalOf { UiSkin.MODERN }
