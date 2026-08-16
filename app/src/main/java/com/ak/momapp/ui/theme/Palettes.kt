package com.ak.momapp.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * The five looks offered under Settings → Personalize. Each palette is
 * just three accent seeds and a light background; the full light and
 * dark schemes are derived with the same blend recipe, so every palette
 * keeps the same contrast and stays readable.
 */
enum class AppPalette(
    private val primarySeed: Color,
    private val secondarySeed: Color,
    private val tertiarySeed: Color,
    private val lightBackground: Color,
    /** Forest and Midnight ignore the system setting and always render dark. */
    val alwaysDark: Boolean = false,
) {
    /** The original terracotta identity, ~25% deeper. */
    CLAY(
        primarySeed = Color(0xFF753823),
        secondarySeed = Color(0xFF3E4A3B),
        tertiarySeed = Color(0xFF584423),
        lightBackground = Color(0xFFF9EFE9),
    ),

    /** Cool light blue with a slate second accent. */
    OCEAN(
        primarySeed = Color(0xFF23577A),
        secondarySeed = Color(0xFF3A5866),
        tertiarySeed = Color(0xFF2F6B66),
        lightBackground = Color(0xFFEFF5F9),
    ),

    /** Fully dark, mossy greens. A calmer dark than Midnight. */
    FOREST(
        primarySeed = Color(0xFF3F7A4C),
        secondarySeed = Color(0xFF4C6B4E),
        tertiarySeed = Color(0xFF2F6B5E),
        lightBackground = Color(0xFFF1F7EE),
        alwaysDark = true,
    ),

    /** Warm amber and spice. */
    HONEY(
        primarySeed = Color(0xFF7A5211),
        secondarySeed = Color(0xFF6E4229),
        tertiarySeed = Color(0xFF5E5426),
        lightBackground = Color(0xFFFBF3E4),
    ),

    /** Fully dark, with soft indigo accents. */
    MIDNIGHT(
        primarySeed = Color(0xFF46519C),
        secondarySeed = Color(0xFF2F7A72),
        tertiarySeed = Color(0xFF71518F),
        lightBackground = Color(0xFFEEEFF5),
        alwaysDark = true,
    );

    /** The three accents, for the swatch dots in the palette picker. */
    val swatch: List<Color>
        get() = listOf(primarySeed, secondarySeed, tertiarySeed)

    fun colors(darkTheme: Boolean, skin: UiSkin = UiSkin.LEGACY): ColorScheme {
        val isDark = darkTheme || alwaysDark
        val base = if (isDark) dark() else light()
        return if (skin == UiSkin.LEGACY) base else base.withModernSurfaces(isDark)
    }

    /**
     * The roles [light] and [dark] never set.
     *
     * Material fills them from its own baseline palette, which is why the
     * Stats chart's zero-day stubs are lilac in every palette today. Modern
     * derives them from the same seed as everything else, so depth and
     * dividers belong to the palette she chose. Legacy keeps the baseline,
     * because Legacy is the look that shipped.
     *
     * The ladder has to be ordered AND distinct: five roles that resolve to
     * the same colour give a surface nothing to sit against, and depth
     * drawn in one flat tone is not depth.
     */
    private fun ColorScheme.withModernSurfaces(isDark: Boolean): ColorScheme =
        if (!isDark) {
            copy(
                background = primarySeed.tint(0.945f),
                surface = primarySeed.tint(0.945f),
                surfaceContainerLowest = primarySeed.tint(0.986f),
                surfaceContainerLow = primarySeed.tint(0.965f),
                surfaceContainer = primarySeed.tint(0.945f),
                surfaceContainerHigh = primarySeed.tint(0.92f),
                surfaceContainerHighest = primarySeed.tint(0.90f),
                outline = primarySeed.tint(0.62f),
                outlineVariant = primarySeed.tint(0.80f),
            )
        } else {
            copy(
                background = primarySeed.shade(0.90f),
                surface = primarySeed.shade(0.90f),
                surfaceContainerLowest = primarySeed.shade(0.92f),
                surfaceContainerLow = primarySeed.shade(0.875f),
                surfaceContainer = primarySeed.shade(0.855f),
                surfaceContainerHigh = primarySeed.shade(0.82f),
                surfaceContainerHighest = primarySeed.shade(0.79f),
                outline = primarySeed.shade(0.50f),
                outlineVariant = primarySeed.shade(0.66f),
            )
        }

    private fun light(): ColorScheme {
        val onLight = primarySeed.shade(0.65f)
        return lightColorScheme(
            primary = primarySeed,
            onPrimary = Color.White,
            primaryContainer = primarySeed.tint(0.8f),
            onPrimaryContainer = primarySeed.shade(0.55f),
            secondary = secondarySeed,
            onSecondary = Color.White,
            secondaryContainer = secondarySeed.tint(0.8f),
            onSecondaryContainer = secondarySeed.shade(0.55f),
            tertiary = tertiarySeed,
            onTertiary = Color.White,
            tertiaryContainer = tertiarySeed.tint(0.8f),
            onTertiaryContainer = tertiarySeed.shade(0.55f),
            background = lightBackground,
            onBackground = onLight,
            surface = lightBackground,
            onSurface = onLight,
            surfaceVariant = primarySeed.tint(0.82f),
            onSurfaceVariant = primarySeed.shade(0.42f),
        )
    }

    private fun dark(): ColorScheme {
        val background = primarySeed.shade(0.88f)
        val onDark = primarySeed.tint(0.87f)
        return darkColorScheme(
            primary = primarySeed.tint(0.55f),
            onPrimary = primarySeed.shade(0.62f),
            primaryContainer = primarySeed.shade(0.35f),
            onPrimaryContainer = primarySeed.tint(0.8f),
            secondary = secondarySeed.tint(0.55f),
            onSecondary = secondarySeed.shade(0.62f),
            secondaryContainer = secondarySeed.shade(0.35f),
            onSecondaryContainer = secondarySeed.tint(0.8f),
            tertiary = tertiarySeed.tint(0.55f),
            onTertiary = tertiarySeed.shade(0.62f),
            tertiaryContainer = tertiarySeed.shade(0.35f),
            onTertiaryContainer = tertiarySeed.tint(0.8f),
            background = background,
            onBackground = onDark,
            surface = background,
            onSurface = onDark,
            surfaceVariant = primarySeed.shade(0.58f),
            onSurfaceVariant = primarySeed.tint(0.72f),
        )
    }
}

private fun Color.tint(fraction: Float): Color = lerp(this, Color.White, fraction)

private fun Color.shade(fraction: Float): Color = lerp(this, Color.Black, fraction)
