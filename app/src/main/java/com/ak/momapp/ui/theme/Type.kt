package com.ak.momapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ak.momapp.R

/**
 * Inter, bundled rather than fetched, so the Modern skin looks the same
 * on a phone with no network and no Google Play.
 *
 * Four static weights instead of one variable file: they behave
 * identically on every device and remove a class of bugs where the
 * weight axis quietly fails to apply. All four carry the full Romanian
 * set including the comma-below s and t, which the system font on an
 * older phone does not reliably have.
 */
val InterFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

/**
 * Tabular figures: every digit takes the same width.
 *
 * This is functional rather than decorative. With proportional digits a
 * 1 is narrower than a 4, so the answer field shifts under her thumb as
 * she types and columns of numbers refuse to line up. Applied to every
 * Modern style, because digits turn up in problem text and button labels
 * as readily as in the answer field, and the setting does nothing at all
 * to letters.
 */
private const val TabularFigures = "tnum"

// Slightly larger body text than Material defaults. The app is used in
// quick glances, so readability beats density everywhere.
val LegacyTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.5.sp,
    ),
)

/**
 * Modern's scale. Weights and tracking are deliberate rather than
 * inherited, and every style asks for tabular figures so the answer field
 * stops reflowing as she types and stat tiles line up.
 *
 * The negative tracking is confined to the large sizes, where Inter set
 * at its default spacing reads loose; body and label sizes keep normal
 * tracking, because tightening small text is what makes it hard to read
 * at exactly the moment she has turned the system font size up.
 */
val ModernTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.Bold,
        fontSize = 44.sp, lineHeight = 48.sp, letterSpacing = (-0.9).sp,
        fontFeatureSettings = TabularFigures,
    ),
    headlineSmall = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.35).sp,
        fontFeatureSettings = TabularFigures,
    ),
    titleLarge = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.2).sp,
        fontFeatureSettings = TabularFigures,
    ),
    titleMedium = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = 0.sp,
        fontFeatureSettings = TabularFigures,
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.Normal,
        fontSize = 18.sp, lineHeight = 26.sp, letterSpacing = 0.sp,
        fontFeatureSettings = TabularFigures,
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 21.sp, letterSpacing = 0.sp,
        fontFeatureSettings = TabularFigures,
    ),
    labelLarge = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.1.sp,
        fontFeatureSettings = TabularFigures,
    ),
    labelSmall = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 0.7.sp,
        fontFeatureSettings = TabularFigures,
    ),
)
