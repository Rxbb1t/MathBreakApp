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

// Slightly larger body text than Material defaults. The app is used in
// quick glances, so readability beats density everywhere.
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.5.sp,
    ),
)
