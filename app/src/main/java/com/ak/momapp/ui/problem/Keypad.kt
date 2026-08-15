package com.ak.momapp.ui.problem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ak.momapp.i18n.LocalStrings
import com.ak.momapp.ui.theme.LocalSkin
import com.ak.momapp.ui.theme.UiSkin
import com.ak.momapp.ui.theme.asControl

/** The longest answer any generator produces is well under this. */
private const val MaxAnswerLength = 9

/** A key's resting height before the accessibility scale is applied. */
private val KeyHeight = 42.dp

/**
 * How much of a key's height the figure on it takes up. Tuned so a 0 and
 * a 9 both sit comfortably inside the key with room above and below.
 */
private const val GlyphFraction = 0.45f

enum class KeypadKey {
    D0, D1, D2, D3, D4, D5, D6, D7, D8, D9, CLEAR, BACKSPACE,
    ;

    /** The figure this key types, or null for the two editing keys. */
    val digit: String?
        get() = if (ordinal <= D9.ordinal) ordinal.toString() else null
}

/**
 * The typed answer after [key] is pressed.
 *
 * Answers are non-negative whole numbers everywhere in the app, so there
 * is no sign to track and no decimal point to get wrong.
 */
fun applyKey(current: String, key: KeypadKey): String = when (key) {
    KeypadKey.CLEAR -> ""
    KeypadKey.BACKSPACE -> current.dropLast(1)
    else -> {
        val digit = key.digit.orEmpty()
        when {
            // Otherwise a stray first tap on 0 leaves "05" on screen.
            current == "0" -> digit
            current.length >= MaxAnswerLength -> current
            else -> current + digit
        }
    }
}

/**
 * The app's own number pad, in place of the system keyboard.
 *
 * It exists because the system keyboard is a poor fit here: it covers
 * half the screen, it offers letters and a minus sign that no answer can
 * use, and which keyboard she gets depends on the phone. This one is
 * always the same and always shows exactly the twelve keys that mean
 * something.
 *
 * The keys are sized in dp through [asControl], not in sp, so they follow
 * her accessibility setting but never the skin's text baseline: Modern's
 * smaller type must not shrink something she has to hit. The figures are
 * measured from the key for the same reason.
 */
@Composable
fun Keypad(
    onKey: (KeypadKey) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val strings = LocalStrings.current
    val keyHeight = KeyHeight.asControl()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(
            listOf(KeypadKey.D1, KeypadKey.D2, KeypadKey.D3),
            listOf(KeypadKey.D4, KeypadKey.D5, KeypadKey.D6),
            listOf(KeypadKey.D7, KeypadKey.D8, KeypadKey.D9),
            listOf(KeypadKey.CLEAR, KeypadKey.D0, KeypadKey.BACKSPACE),
        ).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { key ->
                    Key(
                        key = key,
                        height = keyHeight,
                        enabled = enabled,
                        onKey = onKey,
                        label = when (key) {
                            KeypadKey.CLEAR -> strings.keypadClear
                            KeypadKey.BACKSPACE -> strings.keypadBackspace
                            else -> key.digit.orEmpty()
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun Key(
    key: KeypadKey,
    label: String,
    height: Dp,
    enabled: Boolean,
    onKey: (KeypadKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDigit = key.digit != null
    // Measured from the key rather than the type scale, so the figures
    // stay legible whichever skin is on and whatever her text size is.
    val glyphSize = with(LocalDensity.current) { (height * GlyphFraction).toSp() }
    // One of the few places a token cannot answer on its own. Only Modern
    // defines the surfaceContainer ladder; in Legacy those roles fall
    // through to Material's baseline, which paints stark white keys and
    // lilac editing keys on top of whichever palette she chose. Legacy is
    // frozen, so the keypad asks it for roles it actually defines instead.
    val modern = LocalSkin.current == UiSkin.MODERN
    Surface(
        onClick = { onKey(key) },
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        color = when {
            // The two editing keys read as chrome, not as answers.
            modern && isDigit -> MaterialTheme.colorScheme.surfaceContainerLowest
            modern -> MaterialTheme.colorScheme.surfaceContainerHigh
            isDigit -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            else -> MaterialTheme.colorScheme.secondaryContainer
        },
        contentColor = if (modern || isDigit) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        },
        modifier = modifier
            .height(height)
            .semantics { contentDescription = label },
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                // Digits print themselves; the editing keys get a mark,
                // because their words are far too long to fit on a key.
                text = when (key) {
                    KeypadKey.CLEAR -> "C"
                    KeypadKey.BACKSPACE -> "⌫"
                    else -> label
                },
                fontSize = glyphSize,
                fontWeight = if (isDigit) FontWeight.Medium else FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
        }
    }
}
