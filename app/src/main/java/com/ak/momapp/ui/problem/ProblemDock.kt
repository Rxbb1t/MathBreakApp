package com.ak.momapp.ui.problem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ak.momapp.i18n.LocalStrings
import com.ak.momapp.problem.ComparisonProblemGenerator
import com.ak.momapp.problem.MissingOperatorGenerator
import com.ak.momapp.problem.ProblemKind
import com.ak.momapp.problem.TrueFalseProblemGenerator
import com.ak.momapp.ui.theme.LocalSkin
import com.ak.momapp.ui.theme.UiSkin
import com.ak.momapp.ui.theme.asControl

/**
 * Everything she touches, anchored to the bottom of the screen.
 *
 * The point of gathering these into one block is that they stop moving.
 * Before this, a problem with hints put Skip in one place and a problem
 * without hints put it somewhere else, and the answer row sat at a
 * different height for every kind of problem, because all of it was laid
 * out after content whose height varied. Now the dock takes its natural
 * height at the bottom and the problem above it takes whatever is left,
 * so Hint and Skip are in the same place on every problem of every kind.
 *
 * The quiet row is always present and always last for the same reason.
 * A button that is missing on some problems is a button she has to look
 * for on all of them.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProblemDock(
    uiState: ProblemUiState,
    onKey: (KeypadKey) -> Unit,
    onSubmit: () -> Unit,
    onSubmitChoice: (Int) -> Unit,
    onToggleCard: (Int) -> Unit,
    onUseHint: () -> Unit,
    onSkip: () -> Unit,
    onOpenNotes: () -> Unit,
    onNextProblem: () -> Unit,
    onNewRound: () -> Unit,
    onSnooze: () -> Unit,
    showSnooze: Boolean,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val problem = uiState.problem
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (problem.kind) {
            // One tap answers: < = >, check or cross, or the missing sign.
            ProblemKind.COMPARE, ProblemKind.TRUE_FALSE, ProblemKind.MISSING_OP -> Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                val choices = when (problem.kind) {
                    ProblemKind.TRUE_FALSE -> TrueFalseProblemGenerator.CHOICES
                    ProblemKind.MISSING_OP -> MissingOperatorGenerator.SYMBOLS
                    else -> ComparisonProblemGenerator.SYMBOLS
                }
                choices.forEachIndexed { index, symbol ->
                    FilledTonalButton(
                        onClick = { onSubmitChoice(index) },
                        enabled = !uiState.isFinished,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp.asControl()),
                    ) {
                        Text(text = symbol, fontSize = 30.sp)
                    }
                }
            }

            // Tappable cards; the selection lives in the input. Targets and
            // hunts share the grid. Only the winning condition differs.
            ProblemKind.TARGET, ProblemKind.SELECT -> FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                modifier = Modifier.fillMaxWidth(),
            ) {
                problem.cards.forEachIndexed { index, value ->
                    FilterChip(
                        selected = index.digitToChar() in uiState.input,
                        onClick = { onToggleCard(index) },
                        enabled = !uiState.isFinished,
                        label = {
                            Text(
                                text = "$value",
                                fontSize = 24.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
                            )
                        },
                    )
                }
            }

            else -> {
                AnswerDisplay(uiState = uiState)
                Spacer(Modifier.height(12.dp))
                Keypad(
                    onKey = onKey,
                    enabled = !uiState.isFinished,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        when (uiState.phase) {
            AnswerPhase.ANSWERING, AnswerPhase.TRY_AGAIN -> {
                if (!problem.submitsOnTap) {
                    // A hunt (SELECT) checks with any picks. How many belong
                    // is part of the question.
                    val ready = if (problem.kind == ProblemKind.TARGET) {
                        uiState.input.length == problem.pickCount
                    } else {
                        uiState.input.isNotEmpty()
                    }
                    Button(
                        onClick = onSubmit,
                        enabled = ready,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp.asControl()),
                    ) {
                        Text(strings.check, style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.height(8.dp))
                }
                if (showSnooze) {
                    FilledTonalButton(
                        onClick = onSnooze,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp.asControl()),
                    ) {
                        Text(strings.snooze15, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            AnswerPhase.CORRECT, AnswerPhase.REVEALED -> {
                if (uiState.sessionComplete) {
                    Text(
                        text = strings.breakDone,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                    )
                    // The cap is a suggestion she set, not a lock. Tonal
                    // rather than filled, so the message stays the loud
                    // part: carrying on is offered, not urged.
                    FilledTonalButton(
                        onClick = onNewRound,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp.asControl()),
                    ) {
                        Text(strings.anotherRound, style = MaterialTheme.typography.titleMedium)
                    }
                } else {
                    FilledTonalButton(
                        onClick = onNextProblem,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp.asControl()),
                    ) {
                        Text(strings.oneMore, style = MaterialTheme.typography.titleLarge)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        QuietRow(
            hasHints = problem.hints.isNotEmpty(),
            hasNotes = problem.notes.isNotEmpty(),
            hintsLeft = ProblemViewModel.MAX_HINTS - uiState.hintsUsed,
            enabled = !uiState.isFinished,
            onUseHint = onUseHint,
            onOpenNotes = onOpenNotes,
            onSkip = onSkip,
        )
    }
}

/**
 * Hint, Notes and Skip, in that order, on every problem.
 *
 * A missing button leaves a gap of exactly its own width rather than
 * letting the others slide across to fill it. Skip is the one she reaches
 * for without looking, so it has to be in the same place whether or not
 * this particular problem happens to carry hints.
 */
@Composable
private fun QuietRow(
    hasHints: Boolean,
    hasNotes: Boolean,
    hintsLeft: Int,
    enabled: Boolean,
    onUseHint: () -> Unit,
    onOpenNotes: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (hasHints) {
            QuietButton(
                text = strings.hintButton(hintsLeft),
                enabled = enabled,
                onClick = onUseHint,
                modifier = Modifier.weight(1f),
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
        if (hasNotes) {
            QuietButton(
                text = strings.notebookTitle,
                enabled = true,
                onClick = onOpenNotes,
                modifier = Modifier.weight(1f),
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
        QuietButton(
            text = strings.skipButton,
            enabled = enabled,
            onClick = onSkip,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun QuietButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        modifier = modifier.heightIn(min = 48.dp.asControl()),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

/**
 * The typed answer, shown rather than edited.
 *
 * The system keyboard is gone, so this is a readout, not a text field.
 * In Modern it is the inset counterpart to the raised problem card: the
 * darkest container in the ladder against the card's lightest, which is
 * the only way to read as recessed when Compose has no inner shadow.
 * Legacy keeps its outline, because Legacy keeps its look.
 */
@Composable
private fun AnswerDisplay(
    uiState: ProblemUiState,
    modifier: Modifier = Modifier,
) {
    val modern = LocalSkin.current == UiSkin.MODERN
    val unit = uiState.problem.answerUnit
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (modern) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = if (modern) null else BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp.asControl()),
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                // A placeholder question mark rather than an empty box, so
                // the row never looks like something failed to load.
                text = uiState.input.ifEmpty { "?" },
                fontSize = 34.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = if (uiState.input.isEmpty()) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            // The expected unit ("m", "deg", "lei", "min") sits beside the
            // number so it is never a guess.
            if (unit.isNotEmpty()) {
                Spacer(Modifier.padding(horizontal = 3.dp))
                Text(
                    text = unit,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
