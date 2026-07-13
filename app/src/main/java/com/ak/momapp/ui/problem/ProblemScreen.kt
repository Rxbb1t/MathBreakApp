package com.ak.momapp.ui.problem

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ak.momapp.i18n.LocalStrings
import com.ak.momapp.problem.ComparisonProblemGenerator
import com.ak.momapp.problem.Difficulty
import com.ak.momapp.problem.PersonalContent
import com.ak.momapp.problem.Problem
import com.ak.momapp.problem.ProblemKind
import com.ak.momapp.ui.theme.MomAppTheme
import kotlinx.coroutines.launch

@Composable
fun ProblemScreen(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenChallenge: () -> Unit = {},
    breakSession: Int = 0,
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

    // Every break arrival starts a fresh sitting for the per-break cap
    // (and skips the Start screen — the notification tap was the start).
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
            onUseHint = viewModel::useHint,
            onSkip = viewModel::skipProblem,
            onSubmitChoice = viewModel::submitChoice,
            onToggleCard = viewModel::toggleCard,
            onOpenSettings = onOpenSettings,
            onOpenChallenge = onOpenChallenge,
            challengeDone = challengeDone,
            showSnooze = breakSession > 0,
            onSnooze = onSnooze,
            soundEnabled = soundEnabled,
            sessionDone = sessionDone,
            sessionLimit = sessionLimit,
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
) {
    val strings = LocalStrings.current
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current

    // The helper sheet only exists when the problem brought notes along.
    val notebookAvailable = uiState.problem.notes.isNotEmpty()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    // New problem → focus the field; answered or tap-answered (no field
    // to focus) → tuck the keyboard away.
    LaunchedEffect(uiState.problem, uiState.isFinished) {
        if (uiState.isFinished || uiState.problem.tapAnswered) {
            keyboard?.hide()
        } else {
            focusRequester.requestFocus()
        }
    }
    // Writing space beats the keyboard; bring the keyboard back after.
    LaunchedEffect(drawerState.isOpen) {
        if (drawerState.isOpen) {
            keyboard?.hide()
        } else if (!uiState.isFinished && !uiState.problem.tapAnswered) {
            focusRequester.requestFocus()
        }
    }
    LaunchedEffect(uiState.phase) {
        when (uiState.phase) {
            AnswerPhase.CORRECT -> {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                if (soundEnabled) SuccessChime.play(context)
            }
            AnswerPhase.TRY_AGAIN, AnswerPhase.REVEALED ->
                haptics.performHapticFeedback(HapticFeedbackType.Reject)
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
                    .imePadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                    SuggestionChip(
                        onClick = {},
                        enabled = false,
                        label = { Text(strings.difficultyLabel(uiState.problem.difficulty)) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            disabledContainerColor = chipContainer,
                            disabledLabelColor = chipLabel,
                        ),
                        border = null,
                    )
                    Spacer(Modifier.weight(1f))
                    if (solvedToday > 0) {
                        Text(
                            text = strings.solvedToday(solvedToday),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TrophyButton(challengeDone = challengeDone, onClick = onOpenChallenge)
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = strings.settingsIconDescription,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (sessionLimit > 0) {
                    Spacer(Modifier.height(10.dp))
                    SittingProgress(done = sessionDone, limit = sessionLimit)
                }

                uiState.remainingSeconds?.let { seconds ->
                    if (!uiState.isFinished) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "%d:%02d".format(seconds / 60, seconds % 60),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (seconds <= 10) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }

                // Scrollable middle: with Large text and the keyboard up, a
                // long riddle scrolls instead of colliding with the buttons.
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Spacer(Modifier.height(16.dp))

                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 28.dp),
                        ) {
                            Text(
                                text = uiState.problem.text,
                                style = when (uiState.problem.kind) {
                                    ProblemKind.ARITHMETIC -> MaterialTheme.typography.displayMedium
                                    ProblemKind.EQUATION, ProblemKind.PUZZLE, ProblemKind.COMPARE ->
                                        MaterialTheme.typography.headlineLarge
                                    ProblemKind.WORD, ProblemKind.LOGIC, ProblemKind.GEOMETRY,
                                    ProblemKind.MONEY, ProblemKind.TIME, ProblemKind.TARGET,
                                    ProblemKind.SELECT, ProblemKind.SETS,
                                    -> MaterialTheme.typography.headlineMedium
                                },
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            uiState.problem.diagram?.let { diagram ->
                                Spacer(Modifier.height(20.dp))
                                ProblemDiagram(diagram)
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    when (uiState.problem.kind) {
                        // One tap answers: <, = or >.
                        ProblemKind.COMPARE -> Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            ComparisonProblemGenerator.SYMBOLS.forEachIndexed { index, symbol ->
                                FilledTonalButton(
                                    onClick = { onSubmitChoice(index) },
                                    enabled = !uiState.isFinished,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(64.dp),
                                ) {
                                    Text(text = symbol, fontSize = 30.sp)
                                }
                            }
                        }

                        // Tappable cards; the selection lives in the input.
                        // Targets and hunts share the grid — only the
                        // winning condition differs.
                        ProblemKind.TARGET, ProblemKind.SELECT -> FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            uiState.problem.cards.forEachIndexed { index, value ->
                                FilterChip(
                                    selected = index.digitToChar() in uiState.input,
                                    onClick = { onToggleCard(index) },
                                    enabled = !uiState.isFinished,
                                    label = {
                                        Text(
                                            text = "$value",
                                            fontSize = 24.sp,
                                            modifier = Modifier.padding(
                                                horizontal = 6.dp,
                                                vertical = 10.dp,
                                            ),
                                        )
                                    },
                                )
                            }
                        }

                        else -> AnswerField(
                            uiState = uiState,
                            onInputChange = onInputChange,
                            onSubmit = onSubmit,
                            focusRequester = focusRequester,
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    FeedbackArea(uiState)

                    Spacer(Modifier.height(16.dp))
                }

                when (uiState.phase) {
                    AnswerPhase.ANSWERING, AnswerPhase.TRY_AGAIN -> {
                        // Comparisons submit on tap; everything else checks.
                        if (uiState.problem.kind != ProblemKind.COMPARE) {
                            // A hunt (SELECT) checks with any picks — how
                            // many belong is part of the question.
                            val ready = if (uiState.problem.kind == ProblemKind.TARGET) {
                                uiState.input.length == uiState.problem.pickCount
                            } else {
                                uiState.input.isNotEmpty()
                            }
                            Button(
                                onClick = onSubmit,
                                enabled = ready,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp),
                            ) {
                                Text(strings.check, style = MaterialTheme.typography.titleLarge)
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            // Tap exercises carry no hints, so no Hint button.
                            if (uiState.problem.hints.isNotEmpty()) {
                                FilledTonalButton(
                                    onClick = onUseHint,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp),
                                ) {
                                    Text(
                                        text = strings.hintButton(ProblemViewModel.MAX_HINTS - uiState.hintsUsed),
                                        style = MaterialTheme.typography.titleMedium,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                            FilledTonalButton(
                                onClick = onSkip,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp),
                            ) {
                                Text(
                                    text = strings.skipButton,
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                        if (showSnooze) {
                            Spacer(Modifier.height(8.dp))
                            FilledTonalButton(
                                onClick = onSnooze,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 44.dp),
                            ) {
                                Text(strings.snooze15, style = MaterialTheme.typography.titleMedium)
                            }
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
                                    .padding(vertical = 16.dp),
                            )
                        } else {
                            FilledTonalButton(
                                onClick = onNextProblem,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp),
                            ) {
                                Text(strings.oneMore, style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                }
            }

            // A little tab on the left edge — the tap alternative to the
            // swipe, and the visual hint that the notebook exists.
            if (notebookAvailable) {
                Surface(
                    onClick = { scope.launch { drawerState.open() } },
                    shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.align(Alignment.CenterStart),
                ) {
                    Text(
                        text = "📝",
                        modifier = Modifier.padding(
                            start = 4.dp,
                            end = 6.dp,
                            top = 12.dp,
                            bottom = 12.dp,
                        ),
                    )
                }
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

/** The plain type-a-number field used by every non-tap problem kind. */
@Composable
private fun AnswerField(
    uiState: ProblemUiState,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    focusRequester: FocusRequester,
) {
    OutlinedTextField(
        value = uiState.input,
        onValueChange = onInputChange,
        enabled = !uiState.isFinished,
        textStyle = TextStyle(
            fontSize = 34.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        // The expected unit ("m", "°", "lei", "min") sits in
        // the field so it's never a guess.
        suffix = uiState.problem.answerUnit
            .takeIf(String::isNotEmpty)
            ?.let { unit ->
                {
                    Text(
                        text = unit,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        placeholder = {
            Text(
                text = "?",
                fontSize = 34.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
    )
}

/** The halo behind an unsolved day's trophy — gold in every palette. */
private val TrophyGold = Color(0xFFF5B301)

/**
 * The daily challenge lives behind the little trophy. A golden pulse
 * says "today's is waiting"; once done it rests, greyed out, until
 * tomorrow. Shared by the Start screen and the problem screen.
 */
@Composable
private fun TrophyButton(challengeDone: Boolean, onClick: () -> Unit) {
    val strings = LocalStrings.current
    IconButton(onClick = onClick) {
        if (challengeDone) {
            Text(
                text = "🏆",
                modifier = Modifier
                    .alpha(0.35f)
                    .semantics { contentDescription = strings.challengeTitle },
            )
        } else {
            val pulse = rememberInfiniteTransition(label = "trophy")
            val scale by pulse.animateFloat(
                initialValue = 1f,
                targetValue = 1.14f,
                animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
                label = "trophyScale",
            )
            val glow by pulse.animateFloat(
                initialValue = 0.1f,
                targetValue = 0.4f,
                animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
                label = "trophyGlow",
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(TrophyGold.copy(alpha = glow)),
            ) {
                Text(
                    text = "🏆",
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .semantics { contentDescription = strings.challengeTitle },
                )
            }
        }
    }
}

/**
 * What greets her when she opens the app on her own: the usual top-row
 * buttons and one big Start — no problem until she asks for one.
 */
@Composable
private fun StartContent(
    solvedToday: Int,
    challengeDone: Boolean,
    onStart: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenChallenge: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    Scaffold(modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.weight(1f))
                if (solvedToday > 0) {
                    Text(
                        text = strings.solvedToday(solvedToday),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TrophyButton(challengeDone = challengeDone, onClick = onOpenChallenge)
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = strings.settingsIconDescription,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(text = "🧮", style = MaterialTheme.typography.displayLarge)
                Spacer(Modifier.height(20.dp))
                Text(
                    text = strings.readyLine,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(28.dp))
                Button(
                    onClick = onStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                ) {
                    Text(strings.startButton, style = MaterialTheme.typography.titleLarge)
                }
            }
        }
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
                    text = strings.correctFeedback,
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
                text = if (uiState.timedOut) {
                    strings.timeUpFeedback(uiState.problem.answerText)
                } else {
                    strings.revealedFeedback(uiState.problem.answerText)
                },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProblemScreenPreview() {
    MomAppTheme {
        ProblemScreenContent(
            uiState = ProblemUiState(
                problem = Problem("23 + 48 = ?", 71, Difficulty.EASY),
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
                problem = Problem("23 + 48 = ?", 71, Difficulty.EASY),
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
