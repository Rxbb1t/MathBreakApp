package com.ak.momapp.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.ak.momapp.data.BrainBreakSettings

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
    largeText: Boolean = false,
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
    MaterialTheme(
        colorScheme = palette.colors(isDark),
        typography = if (largeText) {
            Typography.scaled(BrainBreakSettings.LARGE_TEXT_SCALE)
        } else {
            Typography
        },
        shapes = SoftShapes,
        content = content,
    )
}
