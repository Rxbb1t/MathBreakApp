package com.ak.momapp.ui.icons

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The handful of marks Modern draws itself.
 *
 * Legacy keeps its emoji. Emoji are drawn by the system font, which means
 * they are a different illustration on every phone, they ignore the
 * palette entirely, and they carry a colour scheme of their own that
 * nothing here can tune. These take the palette like everything else.
 *
 * Settings and Back are NOT here: Icons.Filled.Settings and
 * Icons.AutoMirrored.Filled.ArrowBack are already Material vectors that
 * tint correctly, so replacing them would buy nothing.
 */
object AppIcons {

    /** The daily challenge. A cup with two handles on a plinth. */
    val Trophy: ImageVector = vector("Trophy") {
        // The bowl, tapering to the stem.
        path(fill = SolidColor(Color.Black)) {
            moveTo(7f, 3f)
            lineTo(17f, 3f)
            lineTo(17f, 8.2f)
            curveTo(17f, 11.4f, 14.8f, 13.2f, 12f, 13.2f)
            curveTo(9.2f, 13.2f, 7f, 11.4f, 7f, 8.2f)
            close()
        }
        // Stem and plinth, as one piece so they cannot drift apart.
        path(fill = SolidColor(Color.Black)) {
            moveTo(11f, 13f)
            lineTo(13f, 13f)
            lineTo(13f, 17f)
            lineTo(16f, 17f)
            lineTo(16f, 19.5f)
            lineTo(8f, 19.5f)
            lineTo(8f, 17f)
            lineTo(11f, 17f)
            close()
        }
        // Handles, drawn as strokes so they read as thin at any size.
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.6f) {
            moveTo(7f, 4.6f)
            lineTo(4.4f, 4.6f)
            curveTo(4.4f, 8.4f, 5.4f, 10.2f, 7.4f, 10.9f)
        }
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.6f) {
            moveTo(17f, 4.6f)
            lineTo(19.6f, 4.6f)
            curveTo(19.6f, 8.4f, 18.6f, 10.2f, 16.6f, 10.9f)
        }
    }

    /**
     * The helper sheet: a page with three rules.
     *
     * Page and rules are ONE path with an even-odd fill, so the rules are
     * genuine holes that let the button colour through. Drawing them as a
     * second white path would paint white rules, which is only correct on
     * a white button and this app never has one.
     */
    val Notebook: ImageVector = vector("Notebook") {
        path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
            moveTo(5f, 2.5f)
            lineTo(14f, 2.5f)
            lineTo(19f, 7.5f)
            lineTo(19f, 21.5f)
            lineTo(5f, 21.5f)
            close()
            moveTo(7.5f, 12f)
            lineTo(16.5f, 12f)
            lineTo(16.5f, 13.4f)
            lineTo(7.5f, 13.4f)
            close()
            moveTo(7.5f, 15.4f)
            lineTo(16.5f, 15.4f)
            lineTo(16.5f, 16.8f)
            lineTo(7.5f, 16.8f)
            close()
            moveTo(7.5f, 18.8f)
            lineTo(13f, 18.8f)
            lineTo(13f, 20.2f)
            lineTo(7.5f, 20.2f)
            close()
        }
    }

    /** Delete one digit. The usual pointed key with a cross cut out of it. */
    val Backspace: ImageVector = vector("Backspace") {
        path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
            moveTo(9f, 4f)
            lineTo(22f, 4f)
            lineTo(22f, 20f)
            lineTo(9f, 20f)
            lineTo(2f, 12f)
            close()
            moveTo(12.6f, 8.2f)
            lineTo(15.5f, 11.1f)
            lineTo(18.4f, 8.2f)
            lineTo(19.8f, 9.6f)
            lineTo(16.9f, 12.5f)
            lineTo(19.8f, 15.4f)
            lineTo(18.4f, 16.8f)
            lineTo(15.5f, 13.9f)
            lineTo(12.6f, 16.8f)
            lineTo(11.2f, 15.4f)
            lineTo(14.1f, 12.5f)
            lineTo(11.2f, 9.6f)
            close()
        }
    }

    private fun vector(
        name: String,
        content: ImageVector.Builder.() -> Unit,
    ): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply(content).build()
}

/**
 * The mark on the Start screen: an abacus, echoing the launcher icon.
 *
 * Drawn rather than set in an emoji font so it takes the palette she
 * chose, and so it is the same drawing on every phone. Sized entirely
 * from [size], with nothing measured in sp, because it is a picture and
 * not a piece of text.
 */
@Composable
fun StartMark(modifier: Modifier = Modifier, size: Dp = 88.dp) {
    val tile = MaterialTheme.colorScheme.primaryContainer
    val ink = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier.size(size)) {
        val side = this.size.minDimension
        val rod = side * 0.055f
        drawRoundRect(
            color = tile,
            size = Size(side, side),
            cornerRadius = CornerRadius(side * 0.26f, side * 0.26f),
        )
        // Three rods with beads pushed to alternating ends, which is what
        // makes it read as an abacus rather than as a grid.
        val beadRadius = side * 0.072f
        val left = side * 0.22f
        val right = side * 0.78f
        listOf(0.32f, 0.5f, 0.68f).forEachIndexed { index, fraction ->
            val y = side * fraction
            drawLine(
                color = ink,
                start = Offset(left, y),
                end = Offset(right, y),
                strokeWidth = rod,
                cap = StrokeCap.Round,
            )
            // Two beads a row, parked at opposite ends on alternate rows.
            val parkedLeft = index % 2 == 0
            val first = if (parkedLeft) left + beadRadius else right - beadRadius
            val step = if (parkedLeft) beadRadius * 2.2f else -beadRadius * 2.2f
            repeat(2) { bead ->
                drawCircle(
                    color = ink,
                    radius = beadRadius,
                    center = Offset(first + step * bead, y),
                )
            }
        }
        // A hairline frame, so the tile has an edge at any palette.
        drawRoundRect(
            color = ink.copy(alpha = 0.25f),
            size = Size(side, side),
            cornerRadius = CornerRadius(side * 0.26f, side * 0.26f),
            style = Stroke(width = side * 0.02f),
        )
    }
}
