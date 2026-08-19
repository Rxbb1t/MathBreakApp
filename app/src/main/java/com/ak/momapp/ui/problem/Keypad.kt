package com.ak.momapp.ui.problem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.ak.momapp.ui.icons.AppIcons
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
 * The answer readout, the keypad under it, and the handle that folds the
 * keypad away.
 *
 * The keypad is the tallest thing on the screen and the question is the
 * thing she came here to read, so on a long word problem the two are in
 * competition. The handle hands her the room back mid-problem, and a
 * finished problem folds the keypad by itself: a pad that no longer
 * accepts a digit has no business covering the working it is being
 * replaced by.
 *
 * The hand-folded state is keyed to [resetKey] -- the problem -- so every
 * new question arrives with the keypad up. Carrying a fold across would
 * leave her facing the next question with nothing to type on until she
 * worked out why.
 */
@Composable
fun KeypadPanel(
    input: String,
    unit: String,
    onInput: (String) -> Unit,
    finished: Boolean,
    modifier: Modifier = Modifier,
    resetKey: Any? = null,
) {
    var foldedByHand by remember(resetKey) { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnswerDisplay(
                input = input,
                unit = unit,
                // Tapping the bar brings up her own keyboard, which lands
                // over the keys below rather than moving them.
                onInput = onInput,
                enabled = !finished,
                modifier = Modifier.weight(1f),
            )
            // The handle goes with the keypad when the problem ends. There
            // is nothing left to unfold, and a control that does nothing is
            // worse than no control at all.
            if (!finished) {
                KeypadHandle(
                    folded = foldedByHand,
                    onClick = { foldedByHand = !foldedByHand },
                )
            }
        }
        AnimatedVisibility(
            visible = !finished && !foldedByHand,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column {
                Spacer(Modifier.height(12.dp))
                // The panel owns the current text, so it is the one place
                // that can turn a key press into the answer it makes.
                Keypad(
                    onKey = { key -> onInput(applyKey(input, key)) },
                    enabled = !finished,
                )
            }
        }
    }
}

/**
 * The fold-away handle, beside the answer rather than under it.
 *
 * Beside is the only place it stays put: below the keypad it would ride
 * up and down the screen as the keypad folds, so the control she wants
 * would be wherever she last left it. Here it keeps the answer field's
 * height and its own place, folded or not.
 */
@Composable
private fun KeypadHandle(
    folded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier
            .size(width = 46.dp.asControl(), height = 64.dp.asControl())
            .semantics {
                contentDescription = if (folded) strings.keypadShow else strings.keypadHide
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (folded) {
                    Icons.Filled.KeyboardArrowUp
                } else {
                    Icons.Filled.KeyboardArrowDown
                },
                contentDescription = null,
            )
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
            // Digits print themselves; the editing keys get a mark,
            // because their words are far too long to fit on a key.
            if (key == KeypadKey.BACKSPACE && modern) {
                // Sanctioned skin branch: the erase mark is a picture. The
                // font's own U+232B renders at whatever weight and shape
                // the system face happens to have, which next to Inter
                // reads as borrowed. Legacy keeps that character.
                Icon(
                    imageVector = AppIcons.Backspace,
                    contentDescription = null,
                    modifier = Modifier.size(with(LocalDensity.current) { glyphSize.toDp() * 1.15f }),
                )
            } else {
                Text(
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
}
