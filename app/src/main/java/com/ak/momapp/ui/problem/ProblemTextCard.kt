package com.ak.momapp.ui.problem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.TextAutoSize
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ak.momapp.problem.Diagram
import com.ak.momapp.ui.theme.AtUserFontScale
import com.ak.momapp.ui.theme.LocalSkin
import com.ak.momapp.ui.theme.ProblemFloorScaleCeiling
import com.ak.momapp.ui.theme.UiSkin

/**
 * As small as the problem text is ever allowed to shrink itself, at a
 * normal system font size.
 *
 * Inside [AtUserFontScale] an sp is multiplied by her full system scale,
 * so this floor would grow with it: at 3x it lands around 57sp, the
 * shrink-to-fit has nowhere to go, and every question ends up scrolling.
 * [problemFontFloor] holds the floor steady in absolute terms so the
 * ceiling can follow her setting without the floor following it too.
 */
private val MinProblemFontSize = 19.sp

/**
 * The shrink floor, corrected for the font scale in force. Above
 * [ProblemFloorScaleCeiling] it keeps its absolute size instead of
 * growing, so long text can still shrink onto one screen however large
 * her text is set.
 *
 * That ceiling is the floor's own, not the skin's chrome clamp, even
 * though the two share a value. Modern's chrome has no clamp, so reading
 * one from the other would let this floor grow without limit and leave a
 * long question unable to shrink to fit.
 */
@Composable
private fun problemFontFloor(): TextUnit {
    val fontScale = LocalDensity.current.fontScale
    return if (fontScale <= ProblemFloorScaleCeiling) {
        MinProblemFontSize
    } else {
        MinProblemFontSize * (ProblemFloorScaleCeiling / fontScale)
    }
}

/**
 * The tinted card the question lives in, shared by the break screen and
 * the daily challenge.
 *
 * The text scales itself down, only as far as it must, to fit the room
 * the card was given -- so a long story at a large system font size
 * still lands on one screen. If even the smallest size doesn't fit (her
 * phone's font size turned right up, say), the card becomes scrollable
 * rather than cutting the question off: the safety net.
 */
@Composable
fun ProblemTextCard(
    text: String,
    baseStyle: TextStyle,
    modifier: Modifier = Modifier,
    prompt: String? = null,
    diagram: Diagram? = null,
) {
    // Latched per question: once this text has proved too long to fit,
    // it stays in scrolling mode, so the two layouts can't flip-flop.
    var overflowed by remember(text) { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // The problem lifts off the background; the answer below it sinks
    // into one. Two directions from a single light source, which is what
    // stops a screen of stacked cards reading as flat. Compose has no
    // inset shadow, so the well under the answer is read from tone alone:
    // it takes the darkest container while this takes the lightest.
    val lifted = LocalSkin.current == UiSkin.MODERN
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = if (lifted) {
            MaterialTheme.colorScheme.surfaceContainerLowest
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        },
        shadowElevation = if (lifted) 6.dp else 0.dp,
        border = if (lifted) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        } else {
            null
        },
        modifier = modifier,
    ) {
        // The question is the one thing that should honour a huge system
        // font size in full: the rest of the app is clamped so it can't
        // crowd her out, but this is what she came to read.
        AtUserFontScale {
            // Read inside the wrapper: out here the density is still the
            // clamped one and the floor would come back uncorrected.
            val floor = problemFontFloor()
            Column(
                Modifier
                    .fillMaxSize()
                    .then(if (overflowed) Modifier.verticalScroll(scrollState) else Modifier)
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // One-tap exercises are bare numbers, so a little
                // instruction says what the taps mean.
                prompt?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                    )
                }
                if (overflowed) {
                    // Already at the floor size; the height is whatever
                    // the words need, and the card scrolls to the rest.
                    Text(
                        text = text,
                        style = baseStyle,
                        fontSize = floor,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Text(
                        text = text,
                        style = baseStyle,
                        autoSize = TextAutoSize.StepBased(
                            minFontSize = floor,
                            maxFontSize = baseStyle.fontSize,
                            stepSize = 1.sp,
                        ),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        onTextLayout = { if (it.hasVisualOverflow) overflowed = true },
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth(),
                    )
                }
                diagram?.let {
                    Spacer(Modifier.height(20.dp))
                    ProblemDiagram(it)
                }
            }
        }
    }
}
