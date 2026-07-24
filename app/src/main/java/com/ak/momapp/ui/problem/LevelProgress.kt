package com.ak.momapp.ui.problem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.ak.momapp.i18n.LocalStrings
import com.ak.momapp.problem.Difficulty
import com.ak.momapp.problem.Level

/**
 * The hidden readout: long-press a level name anywhere in the app and the
 * fine 0–100 position underneath it appears.
 *
 * Everything about the adaptive system is deliberately invisible, because
 * a number on screen is a number to chase and this app is not a game with
 * a score. But "invisible" and "unknowable" are different things, and
 * someone who genuinely wants to know where they are should not have to
 * take it on faith.
 *
 * So it is here, behind a gesture nobody discovers by accident: no hint,
 * no affordance, nothing to stumble into mid-problem. It costs the calm
 * design nothing and it makes the whole mechanism checkable.
 */
@Composable
fun LevelProgressBar(level: Level, modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    val track = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f)
    val fill = MaterialTheme.colorScheme.primary
    val tick = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    // Animated so a level that moved while the bar was closed slides into
    // place rather than appearing already there.
    val progress by animateFloatAsState(level.fraction.toFloat(), label = "level")

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(8.dp),
        ) {
            val radius = CornerRadius(size.height / 2, size.height / 2)
            drawRoundRect(color = track, cornerRadius = radius)
            if (progress > 0f) {
                drawRoundRect(
                    color = fill,
                    size = Size(size.width * progress, size.height),
                    cornerRadius = radius,
                )
            }
            // The two band boundaries, so the number has somewhere to sit.
            for (edge in listOf(Level.EASY_TOP + 1, Level.MEDIUM_TOP + 1)) {
                val x = size.width * (edge / Level.MAX.toFloat())
                drawLine(
                    color = tick,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 2f,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            for (band in Difficulty.entries) {
                Text(
                    text = strings.difficultyLabel(band),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (band == level.band) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    },
                )
            }
        }
        Text(
            text = "${level.points} / ${Level.MAX}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Press and hold this element to reveal something, let go to hide it again.
 * A quick press is passed on to [onTap] instead, so whatever the element
 * normally does still works.
 *
 * Written as ONE gesture detector rather than a `combinedClickable` next to
 * a separate release watcher. Two detectors on one element have to agree
 * about who consumed which event, and when they don't the readout is left
 * showing with no finger on the screen. This one claims the press outright,
 * which also stops a clickable parent (the whole Exercises row) firing
 * underneath it.
 *
 * [onHold] and [onTap] are read through [rememberUpdatedState] so the
 * pointer loop can be keyed on `Unit`: it is started once and torn down
 * with the composable, instead of being cancelled and restarted on every
 * recomposition that hands it a fresh lambda.
 */
@Composable
fun Modifier.holdToReveal(onHold: (Boolean) -> Unit, onTap: () -> Unit = {}): Modifier {
    val hold by rememberUpdatedState(onHold)
    val tap by rememberUpdatedState(onTap)
    return pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown().consume()
            val liftedEarly = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                waitForUpOrCancellation()
            } != null
            if (liftedEarly) {
                tap()
                return@awaitEachGesture
            }
            hold(true)
            // Always paired with the reveal above, including when a parent
            // steals the gesture mid-hold: a bar that cannot be dismissed
            // is worse than one that never opened.
            try {
                waitForUpOrCancellation()
            } finally {
                hold(false)
            }
        }
    }
}

/** The reveal wrapper: shows [level] only while [visible]. */
@Composable
fun LevelProgressReveal(visible: Boolean, level: Level?, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible && level != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        if (level != null) {
            LevelProgressBar(level, modifier.padding(top = 6.dp))
        }
    }
}
