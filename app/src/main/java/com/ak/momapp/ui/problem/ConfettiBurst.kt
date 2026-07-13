package com.ak.momapp.ui.problem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private class Particle(
    val angle: Float,
    val speed: Float,
    val sizeDp: Float,
    val spin: Float,
    val colorIndex: Int,
)

/**
 * A short celebratory confetti burst, redrawn whenever [burstKey] changes
 * to a non-null value. Pure Canvas — no library, and it ignores touch
 * because it draws over the layout without consuming input.
 */
@Composable
fun ConfettiBurst(burstKey: Any?, modifier: Modifier = Modifier) {
    if (burstKey == null) return

    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer,
    )
    val particles = remember(burstKey) {
        val random = Random(burstKey.hashCode())
        List(PARTICLE_COUNT) {
            Particle(
                angle = random.nextFloat() * 2f * Math.PI.toFloat(),
                speed = 0.4f + random.nextFloat() * 0.6f,
                sizeDp = 5f + random.nextFloat() * 5f,
                spin = if (random.nextBoolean()) 1f else -1f,
                colorIndex = random.nextInt(colors.size),
            )
        }
    }
    val progress = remember(burstKey) { Animatable(0f) }
    LaunchedEffect(burstKey) {
        progress.animateTo(1f, tween(durationMillis = 1_000, easing = LinearEasing))
    }

    Canvas(modifier.fillMaxSize()) {
        // progress is read here, in the draw phase, so each animation frame
        // only redraws this canvas instead of recomposing the whole screen.
        val t = progress.value
        if (t <= 0f || t >= 1f) return@Canvas

        // Burst out fast, then drift: distance decelerates, gravity grows.
        val distance = 1f - (1f - t) * (1f - t)
        val origin = Offset(size.width / 2f, size.height * 0.4f)
        val reach = size.minDimension * 0.45f
        val gravity = 180.dp.toPx() * t * t
        val alpha = 1f - t

        particles.forEach { p ->
            val x = origin.x + cos(p.angle) * p.speed * reach * distance
            val y = origin.y + sin(p.angle) * p.speed * reach * distance * 0.8f + gravity
            val side = p.sizeDp.dp.toPx()
            rotate(degrees = p.spin * t * 540f, pivot = Offset(x, y)) {
                drawRect(
                    color = colors[p.colorIndex],
                    topLeft = Offset(x - side / 2f, y - side / 2f),
                    size = Size(side, side * 0.6f),
                    alpha = alpha,
                )
            }
        }
    }
}

private const val PARTICLE_COUNT = 36
