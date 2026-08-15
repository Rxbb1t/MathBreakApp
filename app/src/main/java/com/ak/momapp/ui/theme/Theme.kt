package com.ak.momapp.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

/**
 * The system font scale, before the skin's chrome ceiling clamps it. Only
 * [AtUserFontScale] needs this; everything else wants the clamped value
 * that [LocalDensity] already carries.
 */
private val LocalUserFontScale = staticCompositionLocalOf { 1f }

/**
 * Renders [content] at the full system font size, undoing the chrome
 * clamp. Wrap the problem text and the answer field in this: if she has
 * set her phone to huge text, those are exactly the places where she
 * meant it.
 */
@Composable
fun AtUserFontScale(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val userScale = LocalUserFontScale.current
    if (userScale == density.fontScale) {
        content()
    } else {
        CompositionLocalProvider(
            LocalDensity provides Density(density.density, userScale),
            content = content,
        )
    }
}

// Colors come from the palette picked under Settings → Personalize;
// dynamic (wallpaper-based) color is intentionally not used.
@Composable
fun MomAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    palette: AppPalette = AppPalette.CLAY,
    skin: UiSkin = UiSkin.MODERN,
    content: @Composable () -> Unit,
) {
    val isDark = darkTheme || palette.alwaysDark
    // Midnight renders dark even when the system theme is light, so the
    // status-bar icons must follow the app, not the system.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as? Activity)?.window?.let { window ->
                val insets = WindowCompat.getInsetsController(window, view)
                insets.isAppearanceLightStatusBars = !isDark
                insets.isAppearanceLightNavigationBars = !isDark
            }
        }
    }
    val density = LocalDensity.current
    // Three scales, deliberately separate, because they answer different
    // questions:
    //
    //  - TYPE follows her system setting times the skin's own baseline.
    //    Modern sits a quarter below Legacy, which buys back several lines
    //    of room at a large font size without anything looking shrunken.
    //  - CHROME stops where the skin says. Legacy's rows cannot reflow, so
    //    past half again their size they shove each other off the screen;
    //    Modern's do reflow, so it never stops. AtUserFontScale hands the
    //    full size back to the problem text either way.
    //  - CONTROLS follow the ACCESSIBILITY setting alone. A 25% smaller
    //    type baseline must never shrink a button she has to hit.
    val userScale = density.fontScale * skin.textScale
    val chromeScale = userScale.coerceAtMost(skin.chromeFontScaleCeiling)
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, chromeScale),
        LocalUserFontScale provides userScale,
        LocalControlScale provides controlScaleFor(density.fontScale),
        LocalSkin provides skin,
    ) {
        MaterialTheme(
            colorScheme = palette.colors(isDark),
            typography = skin.typography,
            shapes = skin.shapes,
            content = content,
        )
    }
}
