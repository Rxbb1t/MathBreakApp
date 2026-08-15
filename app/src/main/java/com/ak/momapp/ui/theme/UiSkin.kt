package com.ak.momapp.ui.theme

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
enum class UiSkin {
    /** Exactly the look shipped through v1.6. Frozen on purpose. */
    LEGACY,

    /** Inter, tonal depth, drawn icons. */
    MODERN,
}

val LocalSkin = staticCompositionLocalOf { UiSkin.MODERN }
