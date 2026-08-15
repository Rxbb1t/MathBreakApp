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

/** Text renders at 99% of its nominal size, app-wide. */
private const val TextScale = 0.99f

/**
 * How far the app's chrome follows the system font size before it stops.
 *
 * Labels, chips and buttons live in rows and boxes that have to hold
 * their shape; past roughly half again their normal size they start
 * shoving each other off the screen. The problem text has no such
 * limit — see [AtUserFontScale], which hands the real scale back to the
 * things she actually reads.
 */
private const val MaxChromeFontScale = 1.5f

/**
 * The system font scale, before [MaxChromeFontScale] clamps it. Only
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

// Generously rounded corners everywhere. Part of the cozy look.
private val SoftShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

// Colors come from the palette picked under Settings → Personalize;
// dynamic (wallpaper-based) color is intentionally not used.
@Composable
fun MomAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    palette: AppPalette = AppPalette.CLAY,
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
    // Every sp in the app rendered a hair smaller (1%), which buys back a
    // line or two of room at a large system font size without anything
    // looking shrunken. On top of that the chrome stops growing at
    // MaxChromeFontScale, so headers and buttons keep their shape however
    // large she has set the system text; AtUserFontScale gives the full
    // size back to the problem itself.
    val density = LocalDensity.current
    val userScale = density.fontScale * TextScale
    val chromeScale = userScale.coerceAtMost(MaxChromeFontScale)
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, chromeScale),
        LocalUserFontScale provides userScale,
    ) {
        MaterialTheme(
            colorScheme = palette.colors(isDark),
            typography = LegacyTypography,
            shapes = SoftShapes,
            content = content,
        )
    }
}
