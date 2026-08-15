package com.ak.momapp.ui.problem

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ak.momapp.i18n.LocalStrings
import com.ak.momapp.problem.Difficulty
import com.ak.momapp.problem.Level
import com.ak.momapp.problem.PersonalContent
import com.ak.momapp.problem.Problem
import com.ak.momapp.problem.ProblemKind
import com.ak.momapp.problem.ProblemTopic
import com.ak.momapp.problem.toLevel
import com.ak.momapp.ui.theme.MomAppTheme
import kotlinx.coroutines.launch

@Composable
fun ProblemScreen(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenChallenge: () -> Unit = {},
    onOpenPractice: () -> Unit = {},
    breakSession: Int = 0,
    /** A notification can be put off; a widget tap she chose cannot. */
    canSnooze: Boolean = false,
    onSnooze: () -> Unit = {},
    viewModel: ProblemViewModel = viewModel(factory = ProblemViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsState()
    val solvedToday by viewModel.solvedToday.collectAsState()
    val challengeDone by viewModel.challengeDoneToday.collectAsState()
    val started by viewModel.started.collectAsState()
    val soundEnabled by viewModel.successSound.collectAsState()
    val sessionDone by viewModel.sessionDone.collectAsState()
    val sessionLimit by viewModel.sessionLimit.collectAsState()
    val currentLevel by viewModel.currentLevel.collectAsState()

    // Every break arrival starts a fresh sitting for the per-break cap
    // (and skips the Start screen. The notification tap was the start).
    LaunchedEffect(breakSession) {
        if (breakSession > 0) viewModel.onBreakOpened()
    }

    val state = uiState
    if (!started) {
        StartContent(
            solvedToday = solvedToday,
            challengeDone = challengeDone,
            onStart = viewModel::startSession,
            onOpenSettings = onOpenSettings,
            onOpenChallenge = onOpenChallenge,
            onOpenPractice = onOpenPractice,
            modifier = modifier,
        )
    } else if (state == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        ProblemScreenContent(
            uiState = state,
            solvedToday = solvedToday,
            onInputChange = viewModel::onInputChange,
            onSubmit = viewModel::submit,
            onNextProblem = viewModel::nextProblem,
            onNewRound = viewModel::startNewRound,
            onUseHint = viewModel::useHint,
            onSkip = viewModel::skipProblem,
            onSubmitChoice = viewModel::submitChoice,
            onToggleCard = viewModel::toggleCard,
            onOpenSettings = onOpenSettings,
            onOpenChallenge = onOpenChallenge,
            challengeDone = challengeDone,
            showSnooze = breakSession > 0 && canSnooze,
            onSnooze = onSnooze,
            soundEnabled = soundEnabled,
            sessionDone = sessionDone,
            sessionLimit = sessionLimit,
            currentLevel = currentLevel,
            modifier = modifier,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProblemScreenContent(
    uiState: ProblemUiState,
    solvedToday: Int,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onNextProblem: () -> Unit,
    /** Carries on past the per-break cap, leaving her level alone. */
    onNewRound: () -> Unit = {},
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenChallenge: () -> Unit = {},
    challengeDone: Boolean = false,
    onUseHint: () -> Unit = {},
    onSkip: () -> Unit = {},
    onSubmitChoice: (Int) -> Unit = {},
    onToggleCard: (Int) -> Unit = {},
    showSnooze: Boolean = false,
    onSnooze: () -> Unit = {},
    soundEnabled: Boolean = false,
    sessionDone: Int = 0,
    sessionLimit: Int = 0,
    // Set while she's drilling one type: the header becomes a back arrow
    // and the type's name instead of the trophy and the settings gear.
    practiceTopic: ProblemTopic? = null,
    onExitPractice: () -> Unit = {},
    // Her position on the fine scale, revealed by long-pressing the level
    // chip. Null while it's still loading.
    currentLevel: Level? = null,
) {
    val strings = LocalStrings.current
    // The hidden readout, held open by a finger and nothing else. Not saved
    // across restarts, and now not even across a lifted thumb: it is a peek,
    // not a mode to end up stuck in.
    var showLevelDetail by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current

    // The helper sheet only exists when the problem brought notes along.
    val notebookAvailable = uiState.problem.notes.isNotEmpty()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    // Nothing on this screen opens the system keyboard any more: the
    // answer is typed on the app's own keypad, so there is no IME to
    // hide, to pad around, or to slide the buttons out of the way of.
    LaunchedEffect(uiState.phase) {
        when (uiState.phase) {
            AnswerPhase.CORRECT -> {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                if (soundEnabled) Chimes.play(context, ChimeSound.SUCCESS)
            }
            AnswerPhase.TRY_AGAIN, AnswerPhase.REVEALED -> {
                haptics.performHapticFeedback(HapticFeedbackType.Reject)
                if (soundEnabled) Chimes.play(context, ChimeSound.TRY_AGAIN)
            }
            AnswerPhase.ANSWERING -> Unit
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = notebookAvailable || drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = NotebookPaper,
                modifier = Modifier.fillMaxWidth(0.88f),
            ) {
                NotebookPad(notes = uiState.problem.notes)
            }
        },
        modifier = modifier,
    ) {
        Scaffold(Modifier.fillMaxSize()) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Chrome recedes while she is thinking and returns the
                // moment she answers. Never to zero and never disabled: a
                // control that vanishes reads as a control that is gone.
                val chromeAlpha by animateFloatAsState(
                    targetValue = if (uiState.phase == AnswerPhase.ANSWERING) 0.38f else 1f,
                    label = "chrome",
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { alpha = chromeAlpha },
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Tinted by level, so a difficulty change is visible at
                    // a glance: sage, honey, then terracotta.
                    val (chipContainer, chipLabel) = when (uiState.problem.difficulty) {
                        Difficulty.EASY -> MaterialTheme.colorScheme.secondaryContainer to
                            MaterialTheme.colorScheme.onSecondaryContainer
                        Difficulty.MEDIUM -> MaterialTheme.colorScheme.tertiaryContainer to
                            MaterialTheme.colorScheme.onTertiaryContainer
                        Difficulty.HARD -> MaterialTheme.colorScheme.primaryContainer to
                            MaterialTheme.colorScheme.onPrimaryContainer
                    }
                    val chip = @Composable {
                        SuggestionChip(
                            onClick = {},
                            enabled = false,
                            label = {
                                Text(
                                    text = strings.difficultyLabel(uiState.problem.difficulty),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    // The hidden readout, and only while she
                                    // holds the level name: nothing to
                                    // stumble into mid-problem, nothing on
                                    // screen hinting a number exists, and no
                                    // way to leave it showing over the
                                    // problem she came here to answer.
                                    modifier = Modifier.holdToReveal(
                                        onHold = { showLevelDetail = it },
                                    ),
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                disabledContainerColor = chipContainer,
                                disabledLabelColor = chipLabel,
                            ),
                            border = null,
                        )
                    }
                    if (practiceTopic != null) {
                        IconButton(onClick = onExitPractice) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = strings.back,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        // The topic name is the flexible one here: it can
                        // ellipsize, the level chip can't usefully.
                        Text(
                            text = strings.topicLabel(practiceTopic),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        chip()
                    } else {
                        // The icons are laid out first and never shrink, so
                        // the gear cannot be pushed off the edge however
                        // large the system font is; the chip gives way
                        // instead. This row used to be four inflexible
                        // children, and at big font sizes the settings
                        // button fell off the right of the screen.
                        Box(Modifier.weight(1f)) { chip() }
                        TrophyButton(challengeDone = challengeDone, onClick = onOpenChallenge)
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = strings.settingsIconDescription,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                // The long-press readout. Sits under the header rather than
                // over the problem, so revealing it never covers the thing
                // she is trying to answer.
                LevelProgressReveal(
                    visible = showLevelDetail,
                    level = currentLevel ?: uiState.problem.level,
                )

                // A drill has no cap, so no dots.
                if (practiceTopic == null) {
                    if (sessionLimit > 0) {
                        Spacer(Modifier.height(10.dp))
                        SittingProgress(done = sessionDone, limit = sessionLimit)
                    }
                    // The day's tally used to ride in the header, where it
                    // was the widest thing in the row and crowded the
                    // buttons out. It reads better under the dots anyway:
                    // both are "how far along am I".
                    if (solvedToday > 0) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = strings.solvedToday(solvedToday),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                // The problem and its feedback take whatever room the dock
                // leaves. The problem text scales itself down to fit, and
                // scrolls only if even the smallest size overflows.
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Short expressions get the big showy size; the text
                    // then shrinks on its own, only as far as it must.
                    val baseStyle = when (uiState.problem.kind) {
                        ProblemKind.ARITHMETIC -> MaterialTheme.typography.displayMedium
                        ProblemKind.EQUATION, ProblemKind.PUZZLE, ProblemKind.COMPARE,
                        ProblemKind.TRUE_FALSE, ProblemKind.MISSING_OP,
                        ProblemKind.ESTIMATE,
                        ->
                            MaterialTheme.typography.headlineLarge
                        ProblemKind.WORD, ProblemKind.LOGIC, ProblemKind.GEOMETRY,
                        ProblemKind.MONEY, ProblemKind.TIME, ProblemKind.TARGET,
                        ProblemKind.SELECT, ProblemKind.SETS,
                        -> MaterialTheme.typography.headlineMedium
                    }
                    ProblemTextCard(
                        text = uiState.problem.text,
                        baseStyle = baseStyle,
                        // Tap kinds say which buttons to use; an estimate
                        // has to say that a near answer counts, or she will
                        // sit there working it out exactly and never find
                        // out that she did not have to.
                        prompt = strings.tapPrompt(uiState.problem.kind).takeIf {
                            uiState.problem.submitsOnTap ||
                                uiState.problem.kind == ProblemKind.ESTIMATE
                        },
                        diagram = uiState.problem.diagram,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    )

                    Spacer(Modifier.height(20.dp))

                    FeedbackArea(uiState = uiState)
                }

                Spacer(Modifier.height(12.dp))

                // Everything she touches, in one block that keeps its
                // place. The keypad replaces the system keyboard, so the
                // screen no longer has to make room for something it does
                // not control the size of.
                ProblemDock(
                    uiState = uiState,
                    onKey = { key -> onInputChange(applyKey(uiState.input, key)) },
                    onSubmit = onSubmit,
                    onSubmitChoice = onSubmitChoice,
                    onToggleCard = onToggleCard,
                    onUseHint = onUseHint,
                    onSkip = onSkip,
                    onOpenNotes = { scope.launch { drawerState.open() } },
                    onNextProblem = onNextProblem,
                    onNewRound = onNewRound,
                    onSnooze = onSnooze,
                    showSnooze = showSnooze,
                )
            }

            ConfettiBurst(
                burstKey = uiState.problem.takeIf { uiState.phase == AnswerPhase.CORRECT },
            )
        }
        }
    }
}

/**
 * How far into a capped sitting she is: one dot per problem, filled as
 * they finish. Larger custom caps fall back to a "3 / 20" counter so the
 * row never overflows.
 */
@Composable
private fun SittingProgress(done: Int, limit: Int, modifier: Modifier = Modifier) {
    if (limit <= 10) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            modifier = modifier,
        ) {
            repeat(limit) { index ->
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            if (index < done) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                            },
                        ),
                )
            }
        }
    } else {
        Text(
            text = "${done.coerceAtMost(limit)} / $limit",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
    }
}

@Composable
private fun FeedbackArea(uiState: ProblemUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val strings = LocalStrings.current
        AnimatedVisibility(
            visible = uiState.phase == AnswerPhase.CORRECT,
            enter = scaleIn(spring(dampingRatio = 0.4f)) + fadeIn(),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    // An estimate she landed near rather than on is still
                    // right, and is told the exact number it was near.
                    text = if (uiState.closeEnough) {
                        strings.closeEnoughFeedback(uiState.problem.answerText)
                    } else {
                        strings.correctFeedback
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                )
                // Resolved against the current language so a toggle in
                // settings retranslates even an already-shown phrase.
                val encouragements = PersonalContent.encouragements(strings.language)
                uiState.encouragementSeed?.let { seed ->
                    if (encouragements.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = encouragements[seed % encouragements.size],
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        if (uiState.phase == AnswerPhase.TRY_AGAIN) {
            Text(
                text = strings.tryAgainFeedback,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.tertiary,
                textAlign = TextAlign.Center,
            )
        }

        // The latest Hint-button nudge stays up until the problem is done.
        uiState.currentHint?.let { hint ->
            if (!uiState.isFinished) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = hint,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        if (uiState.phase == AnswerPhase.REVEALED) {
            Text(
                text = strings.revealedFeedback(uiState.problem.answerText),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        if (uiState.isFinished && uiState.problem.solution.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            WorkedSolution(steps = uiState.problem.solution)
        }
    }
}

/**
 * The working, folded away behind a button and opened only if she wants
 * it. Offered after a right answer as much as after a revealed one: a
 * lucky guess and a confident one look the same from here, and the
 * steps are worth as much either way.
 *
 * Closed by default. Someone who solved it cleanly shouldn't have to
 * scroll past an explanation to reach the next problem.
 */
@Composable
private fun WorkedSolution(steps: List<String>, modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    var open by rememberSaveable(steps) { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TextButton(onClick = { open = !open }) {
            Text(
                text = if (open) strings.hideSolution else strings.showSolution,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        AnimatedVisibility(
            visible = open,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    steps.forEachIndexed { index, step ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                text = "${index + 1}.",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 10.dp),
                            )
                            Text(
                                text = step,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProblemScreenPreview() {
    MomAppTheme {
        ProblemScreenContent(
            uiState = ProblemUiState(
                problem = Problem("23 + 48 = ?", 71, Difficulty.EASY.toLevel()),
            ),
            solvedToday = 0,
            onInputChange = {},
            onSubmit = {},
            onNextProblem = {},
            onOpenSettings = {},
        )
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProblemScreenCorrectDarkPreview() {
    MomAppTheme {
        ProblemScreenContent(
            uiState = ProblemUiState(
                problem = Problem("23 + 48 = ?", 71, Difficulty.EASY.toLevel()),
                input = "71",
                phase = AnswerPhase.CORRECT,
                encouragementSeed = 1,
            ),
            solvedToday = 3,
            onInputChange = {},
            onSubmit = {},
            onNextProblem = {},
            onOpenSettings = {},
        )
    }
}
