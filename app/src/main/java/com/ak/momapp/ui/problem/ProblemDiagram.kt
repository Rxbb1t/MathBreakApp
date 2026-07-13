package com.ak.momapp.ui.problem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.ak.momapp.problem.Diagram
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tan

/**
 * Draws a [Diagram] in the current palette's colors: shapes in the
 * primary accent, labels in the body color. Everything is schematic —
 * proportions are clamped so a 60×8 garden still reads as a rectangle.
 */
@Composable
fun ProblemDiagram(diagram: Diagram, modifier: Modifier = Modifier) {
    val pen = Pen(
        measurer = rememberTextMeasurer(),
        style = MaterialTheme.typography.labelLarge.copy(
            color = MaterialTheme.colorScheme.onBackground,
        ),
        color = MaterialTheme.colorScheme.primary,
    )
    // A single row of boxes doesn't need the full figure height.
    val height = if (diagram is Diagram.SequenceRow) 64.dp else 170.dp
    Canvas(
        modifier
            .fillMaxWidth()
            .height(height),
    ) {
        when (diagram) {
            is Diagram.Rectangle -> rectangleDiagram(diagram, pen)
            is Diagram.Square -> squareDiagram(diagram, pen)
            is Diagram.RightTriangle -> rightTriangleDiagram(diagram, pen)
            is Diagram.AngleTriangle -> angleTriangleDiagram(diagram, pen)
            is Diagram.Box -> boxDiagram(diagram, pen)
            is Diagram.SequenceRow -> sequenceRowDiagram(diagram, pen)
        }
    }
}

/** The shared drawing kit: label measuring plus the two colors. */
private class Pen(
    val measurer: TextMeasurer,
    val style: TextStyle,
    val color: Color,
)

private fun Pen.width(text: String): Float =
    measurer.measure(text, style).size.width.toFloat()

private fun Pen.height(text: String): Float =
    measurer.measure(text, style).size.height.toFloat()

/** Draws [text] centered on [center]. */
private fun DrawScope.labelAt(pen: Pen, text: String, center: Offset) {
    val layout = pen.measurer.measure(text, pen.style)
    drawText(
        textLayoutResult = layout,
        topLeft = Offset(
            center.x - layout.size.width / 2f,
            center.y - layout.size.height / 2f,
        ),
    )
}

private fun DrawScope.line() = Stroke(width = 2.dp.toPx())

private fun DrawScope.dashed() = Stroke(
    width = 2.dp.toPx(),
    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
)

private fun DrawScope.rectangleDiagram(d: Diagram.Rectangle, pen: Pen) {
    val gap = 6.dp.toPx()
    val ringOffset = 8.dp.toPx()
    val leftInset = pen.width(d.heightLabel) + gap * 2
    val bottomInset = pen.height(d.widthLabel) + gap * 2
    val ringInset = if (d.lapsLabel != null) ringOffset + gap + pen.height(d.lapsLabel) else 0f
    val topInset = gap + ringInset
    val rightInset = gap + if (d.lapsLabel != null) ringOffset + gap else 0f

    val availW = size.width - leftInset - rightInset
    val availH = size.height - topInset - bottomInset
    val aspect = d.aspect.coerceIn(1.25f, 2.4f)
    val w = min(availW, availH * aspect)
    val h = w / aspect
    val left = leftInset + (availW - w) / 2f
    val top = topInset + (availH - h) / 2f

    drawRect(pen.color, topLeft = Offset(left, top), size = Size(w, h), style = line())

    if (d.grid) {
        val faint = pen.color.copy(alpha = 0.3f)
        val cell = h / 3f
        var x = left + cell
        while (x < left + w - cell / 2f) {
            drawLine(faint, Offset(x, top), Offset(x, top + h))
            x += cell
        }
        var y = top + cell
        while (y < top + h - cell / 2f) {
            drawLine(faint, Offset(left, y), Offset(left + w, y))
            y += cell
        }
    }

    d.innerLabel?.let { labelAt(pen, it, Offset(left + w / 2f, top + h / 2f)) }

    if (d.lapsLabel != null) {
        // The dashed ring is the path around the shape; "× n" counts laps.
        drawRoundRect(
            color = pen.color.copy(alpha = 0.6f),
            topLeft = Offset(left - ringOffset, top - ringOffset),
            size = Size(w + 2 * ringOffset, h + 2 * ringOffset),
            cornerRadius = CornerRadius(10.dp.toPx()),
            style = dashed(),
        )
        labelAt(
            pen,
            d.lapsLabel,
            Offset(left + w / 2f, top - ringOffset - gap - pen.height(d.lapsLabel) / 2f),
        )
    }

    labelAt(pen, d.widthLabel, Offset(left + w / 2f, top + h + bottomInset / 2f))
    labelAt(pen, d.heightLabel, Offset(left - leftInset / 2f, top + h / 2f))
}

private fun DrawScope.squareDiagram(d: Diagram.Square, pen: Pen) {
    val gap = 6.dp.toPx()
    val ringOffset = 9.dp.toPx()
    val leftInset = gap + ringOffset
    val topInset = gap + ringOffset
    val rightInset = pen.width(d.sideLabel) + gap * 2 + ringOffset
    val bottomInset = pen.height(d.edgeLabel) + gap * 2 + ringOffset

    val side = min(size.width - leftInset - rightInset, size.height - topInset - bottomInset)
    val left = leftInset + (size.width - leftInset - rightInset - side) / 2f
    val top = topInset + (size.height - topInset - bottomInset - side) / 2f

    drawRect(pen.color, topLeft = Offset(left, top), size = Size(side, side), style = line())
    // The dashed ring is the measured edge: the fence, the trim, the hem.
    drawRoundRect(
        color = pen.color.copy(alpha = 0.6f),
        topLeft = Offset(left - ringOffset, top - ringOffset),
        size = Size(side + 2 * ringOffset, side + 2 * ringOffset),
        cornerRadius = CornerRadius(10.dp.toPx()),
        style = dashed(),
    )
    labelAt(
        pen,
        d.sideLabel,
        Offset(left + side + ringOffset + gap + pen.width(d.sideLabel) / 2f, top + side / 2f),
    )
    labelAt(
        pen,
        d.edgeLabel,
        Offset(left + side / 2f, top + side + ringOffset + gap + pen.height(d.edgeLabel) / 2f),
    )
}

private fun DrawScope.rightTriangleDiagram(d: Diagram.RightTriangle, pen: Pen) {
    val gap = 6.dp.toPx()
    val leftInset = pen.width(d.slantLabel) / 2f + gap
    val topInset = pen.height(d.slantLabel) + gap
    val rightInset = pen.width(d.rightLabel) + gap * 2
    val bottomInset = pen.height(d.bottomLabel) + gap * 2

    val availW = size.width - leftInset - rightInset
    val availH = size.height - topInset - bottomInset
    val aspect = d.aspect.coerceIn(0.7f, 1.9f)
    val w = min(availW, availH * aspect)
    val h = w / aspect
    val x0 = leftInset + (availW - w) / 2f
    val x1 = x0 + w
    val y1 = topInset + (availH - h) / 2f + h
    val y0 = y1 - h

    val path = Path().apply {
        moveTo(x0, y1)
        lineTo(x1, y1)
        lineTo(x1, y0)
        close()
    }
    drawPath(path, pen.color, style = line())

    // The little square marks the right angle between ground and wall.
    val mark = 10.dp.toPx()
    drawLine(pen.color, Offset(x1 - mark, y1), Offset(x1 - mark, y1 - mark), strokeWidth = 1.5.dp.toPx())
    drawLine(pen.color, Offset(x1 - mark, y1 - mark), Offset(x1, y1 - mark), strokeWidth = 1.5.dp.toPx())

    labelAt(pen, d.bottomLabel, Offset((x0 + x1) / 2f, y1 + bottomInset / 2f))
    labelAt(pen, d.rightLabel, Offset(x1 + rightInset / 2f, (y0 + y1) / 2f))
    // The slant label floats just above the hypotenuse's midpoint.
    val hypLength = kotlin.math.hypot(w, h)
    val normal = Offset(-h / hypLength, -w / hypLength)
    val lift = 10.dp.toPx() + pen.height(d.slantLabel) / 2f
    labelAt(
        pen,
        d.slantLabel,
        Offset((x0 + x1) / 2f + normal.x * lift, (y0 + y1) / 2f + normal.y * lift),
    )
}

private fun DrawScope.angleTriangleDiagram(d: Diagram.AngleTriangle, pen: Pen) {
    val gap = 6.dp.toPx()
    val topInset = pen.height(d.topLabel) + gap * 2
    val sideInset = 10.dp.toPx()
    val bottomInset = gap

    // Build the triangle with the real base angles over a unit base…
    val alpha = Math.toRadians(d.leftDegrees.toDouble())
    val beta = Math.toRadians(d.rightDegrees.toDouble())
    val apexX = (tan(beta) / (tan(alpha) + tan(beta))).toFloat()
    val apexY = (tan(alpha) * apexX).toFloat()
    val minX = min(0f, apexX)
    val boundsW = maxOf(1f, apexX) - minX
    // …then scale it uniformly into the content area.
    val availW = size.width - sideInset * 2
    val availH = size.height - topInset - bottomInset
    val s = min(availW / boundsW, availH / apexY)
    val offsetX = sideInset + (availW - boundsW * s) / 2f - minX * s
    val baseY = topInset + (availH + apexY * s) / 2f

    val leftV = Offset(offsetX, baseY)
    val rightV = Offset(offsetX + s, baseY)
    val apexV = Offset(offsetX + apexX * s, baseY - apexY * s)

    val path = Path().apply {
        moveTo(leftV.x, leftV.y)
        lineTo(rightV.x, rightV.y)
        lineTo(apexV.x, apexV.y)
        close()
    }
    drawPath(path, pen.color, style = line())

    // Arcs mark the three angles; canvas angles run clockwise, y down.
    val r = 14.dp.toPx()
    val arcStroke = Stroke(width = 1.5.dp.toPx())
    fun arcAt(vertex: Offset, startAngle: Float, sweep: Float) = drawArc(
        color = pen.color,
        startAngle = startAngle,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = Offset(vertex.x - r, vertex.y - r),
        size = Size(2 * r, 2 * r),
        style = arcStroke,
    )
    arcAt(leftV, -d.leftDegrees.toFloat(), d.leftDegrees.toFloat())
    arcAt(rightV, 180f, d.rightDegrees.toFloat())
    val toRight = Math.toDegrees(
        atan2((rightV.y - apexV.y).toDouble(), (rightV.x - apexV.x).toDouble()),
    ).toFloat()
    val toLeft = Math.toDegrees(
        atan2((leftV.y - apexV.y).toDouble(), (leftV.x - apexV.x).toDouble()),
    ).toFloat()
    arcAt(apexV, toRight, toLeft - toRight)

    // Labels sit inside, along each corner's bisector; apex label above.
    val reach = r + 15.dp.toPx()
    fun bisectorLabel(text: String, vertex: Offset, angleDegrees: Float) {
        val rad = Math.toRadians(angleDegrees.toDouble())
        labelAt(
            pen,
            text,
            Offset(vertex.x + reach * cos(rad).toFloat(), vertex.y + reach * sin(rad).toFloat()),
        )
    }
    bisectorLabel(d.leftLabel, leftV, -d.leftDegrees / 2f)
    bisectorLabel(d.rightLabel, rightV, 180f + d.rightDegrees / 2f)
    labelAt(pen, d.topLabel, Offset(apexV.x, apexV.y - gap - pen.height(d.topLabel) / 2f))
}

private fun DrawScope.sequenceRowDiagram(d: Diagram.SequenceRow, pen: Pen) {
    val gap = 8.dp.toPx()
    val n = d.values.size
    val boxW = ((size.width - gap * (n - 1)) / n).coerceAtMost(64.dp.toPx())
    val boxH = size.height.coerceAtMost(boxW)
    val left = (size.width - boxW * n - gap * (n - 1)) / 2f
    val top = (size.height - boxH) / 2f
    val corner = CornerRadius(10.dp.toPx())

    d.values.forEachIndexed { index, value ->
        val boxLeft = left + index * (boxW + gap)
        val isUnknown = index == n - 1
        if (isUnknown) {
            // The missing one: softly filled and dashed, waiting for her.
            drawRoundRect(
                color = pen.color.copy(alpha = 0.12f),
                topLeft = Offset(boxLeft, top),
                size = Size(boxW, boxH),
                cornerRadius = corner,
            )
            drawRoundRect(
                color = pen.color,
                topLeft = Offset(boxLeft, top),
                size = Size(boxW, boxH),
                cornerRadius = corner,
                style = dashed(),
            )
        } else {
            drawRoundRect(
                color = pen.color,
                topLeft = Offset(boxLeft, top),
                size = Size(boxW, boxH),
                cornerRadius = corner,
                style = line(),
            )
        }
        labelAt(pen, value, Offset(boxLeft + boxW / 2f, top + boxH / 2f))
    }
}

private fun DrawScope.boxDiagram(d: Diagram.Box, pen: Pen) {
    val gap = 6.dp.toPx()
    val leftInset = pen.width(d.heightLabel) + gap * 2
    val bottomInset = pen.height(d.lengthLabel) + gap * 2
    val topInset = pen.height(d.depthLabel) + gap * 2
    val rightInset = pen.width(d.depthLabel) + gap * 2

    val availW = size.width - leftInset - rightInset
    val availH = size.height - topInset - bottomInset
    // Front face 1.5:1, receding edges at 0.45 of the face height.
    val fh = min(availH / 1.45f, availW / 1.95f)
    val fw = fh * 1.5f
    val depth = fh * 0.45f
    val x0 = leftInset + (availW - fw - depth) / 2f
    val y0 = topInset + depth + (availH - fh - depth) / 2f

    val frontTopLeft = Offset(x0, y0)
    val frontTopRight = Offset(x0 + fw, y0)
    val frontBottomRight = Offset(x0 + fw, y0 + fh)
    val backTopLeft = frontTopLeft + Offset(depth, -depth)
    val backTopRight = frontTopRight + Offset(depth, -depth)
    val backBottomRight = frontBottomRight + Offset(depth, -depth)

    drawRect(pen.color, topLeft = frontTopLeft, size = Size(fw, fh), style = line())
    drawLine(pen.color, frontTopLeft, backTopLeft, strokeWidth = 2.dp.toPx())
    drawLine(pen.color, frontTopRight, backTopRight, strokeWidth = 2.dp.toPx())
    drawLine(pen.color, frontBottomRight, backBottomRight, strokeWidth = 2.dp.toPx())
    drawLine(pen.color, backTopLeft, backTopRight, strokeWidth = 2.dp.toPx())
    drawLine(pen.color, backTopRight, backBottomRight, strokeWidth = 2.dp.toPx())

    labelAt(pen, d.lengthLabel, Offset(x0 + fw / 2f, y0 + fh + bottomInset / 2f))
    labelAt(pen, d.heightLabel, Offset(x0 - leftInset / 2f, y0 + fh / 2f))
    // The depth label rides the receding top-right edge.
    val edgeMid = (frontTopRight + backTopRight) / 2f
    labelAt(
        pen,
        d.depthLabel,
        Offset(edgeMid.x + gap + pen.width(d.depthLabel) / 2f, edgeMid.y - gap - pen.height(d.depthLabel) / 2f),
    )
}
