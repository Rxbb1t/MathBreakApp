package com.ak.momapp.ui.challenge

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ak.momapp.data.SettingsRepository
import com.ak.momapp.i18n.LocalStrings
import com.ak.momapp.ui.problem.CheckButtonScale
import com.ak.momapp.ui.problem.ConfettiBurst
import com.ak.momapp.ui.problem.KeypadPanel
import com.ak.momapp.ui.problem.NotebookTabAlignment
import com.ak.momapp.ui.problem.QuietButtonScale
import com.ak.momapp.ui.problem.applyKey
import com.ak.momapp.ui.problem.scaledBy
import com.ak.momapp.ui.problem.NotebookPad
import com.ak.momapp.ui.problem.NotebookPaper
import com.ak.momapp.ui.problem.ProblemTextCard
import com.ak.momapp.ui.problem.ChimeSound
import com.ak.momapp.ui.problem.Chimes
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * The daily challenge: one five-stage story a day, no timer, no strikes.
 * Progress dots show how deep into the story she is; each solved stage
 * gets its own little confetti, the finale a big finish card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChallengeViewModel = viewModel(factory = ChallengeViewModel.Factory),
) {
    val strings = LocalStrings.current
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val soundEnabled by remember {
        SettingsRepository(context.applicationContext).settings.map { it.successSound }
    }.collectAsState(initial = false)
    // Each solved stage bumps the celebrations counter: confetti + chime.
    // The finale gets its own little fanfare instead of the plain ding.
    LaunchedEffect(uiState?.celebrations) {
        val state = uiState ?: return@LaunchedEffect
        if (state.celebrations > 0 && soundEnabled) {
            val sound = if (state.phase == ChallengePhase.COMPLETE) ChimeSound.FANFARE else ChimeSound.SUCCESS
            Chimes.play(context, sound)
        }
    }
    LaunchedEffect(uiState?.phase) {
        if (uiState?.phase == ChallengePhase.TRY_AGAIN && soundEnabled) {
            Chimes.play(context, ChimeSound.TRY_AGAIN)
        }
    }

    // The chain leans on four ideas she may want to look up mid-step, so it
    // gets the same left-edge helper sheet the breaks have. The finished
    // card has nothing left to explain, so the drawer goes with the stage.
    val notes = uiState
        ?.takeIf { it.phase != ChallengePhase.COMPLETE }
        ?.stage?.notes.orEmpty()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = notes.isNotEmpty() || drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = NotebookPaper,
                modifier = Modifier.fillMaxWidth(0.88f),
            ) {
                NotebookPad(notes = notes)
            }
        },
        modifier = modifier,
    ) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(strings.challengeTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                },
            )
        },
    ) { innerPadding ->
        val state = uiState
        if (state == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (state.phase == ChallengePhase.COMPLETE) {
                    // A fresh completion (confetti fired) drifts back to the
                    // main screen by itself; a revisit stays until she leaves.
                    if (state.celebrations > 0) {
                        LaunchedEffect(Unit) {
                            delay(5_000)
                            onBack()
                        }
                    }
                    CompletedContent(state)
                } else {
                    StageContent(
                        state = state,
                        onInputChange = viewModel::onInputChange,
                        onSubmit = viewModel::submit,
                        onUseHint = viewModel::useHint,
                        onNextStage = viewModel::nextStage,
                    )
                }
                // The same left-edge tab the breaks have. Without it the
                // sheet is reachable only by a swipe nobody is told about,
                // and this chain is the one place in the app where looking
                // a definition up mid-question is the expected move.
                if (notes.isNotEmpty()) {
                    Surface(
                        onClick = { scope.launch { drawerState.open() } },
                        shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        // Same place as on a break, and for the same
                        // reason: on the story card, not on the answer.
                        modifier = Modifier.align(NotebookTabAlignment),
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
                    burstKey = state.celebrations.takeIf { it > 0 },
                )
            }
        }
    }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StageContent(
    state: ChallengeUiState,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onUseHint: () -> Unit,
    onNextStage: () -> Unit,
) {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StageDots(current = state.stageIndex, total = state.stages.size)
        Spacer(Modifier.height(6.dp))
        Text(
            text = strings.challengeStage(state.stageIndex + 1, state.stages.size),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // No scrolling: the story, the field, and the verdict fit the space
        // between the dots and the button, the story text shrinking itself
        // as far as it must so the whole stage stays on screen at once.
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = state.intro,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))

            ProblemTextCard(
                text = state.stage.text,
                baseStyle = MaterialTheme.typography.headlineMedium,
                diagram = state.stage.diagram,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            // Same readout, keypad and fold-away handle as a break, so the
            // two screens work the same way. It matters more here than
            // anywhere: a challenge stage is a paragraph, and the keypad
            // is exactly the height of the part she cannot read.
            KeypadPanel(
                input = state.input,
                unit = state.stage.answerUnit,
                onKey = { key -> onInputChange(applyKey(state.input, key)) },
                finished = state.phase == ChallengePhase.STAGE_DONE,
                // A new stage is a new question: the pad comes back up.
                resetKey = state.stageIndex,
            )

            Spacer(Modifier.height(12.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when {
                    state.phase == ChallengePhase.STAGE_DONE -> Text(
                        text = strings.correctFeedback,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center,
                    )

                    state.phase == ChallengePhase.TRY_AGAIN -> Text(
                        text = strings.tryAgainFeedback,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.tertiary,
                        textAlign = TextAlign.Center,
                    )
                }
                state.currentHint?.let { hint ->
                    if (state.phase != ChallengePhase.STAGE_DONE) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        if (state.phase == ChallengePhase.STAGE_DONE) {
            FilledTonalButton(
                onClick = onNextStage,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp),
            ) {
                Text(strings.challengeContinue, style = MaterialTheme.typography.titleLarge)
            }
        } else {
            // Check and Hint are sized from the break screen's constants,
            // not from numbers of their own: the same two buttons on two
            // screens have to come out the same size.
            Button(
                onClick = onSubmit,
                enabled = state.input.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp * CheckButtonScale),
            ) {
                Text(
                    text = strings.check,
                    style = MaterialTheme.typography.titleLarge.scaledBy(CheckButtonScale),
                )
            }
            // Always present now. There is no keyboard left to slide it
            // out of the way of, and a Hint button that comes and goes is
            // one she has to look for every time.
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(
                onClick = onUseHint,
                enabled = state.hintsUsed < ChallengeViewModel.MAX_HINTS,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp * QuietButtonScale),
            ) {
                Text(
                    text = strings.hintButton(ChallengeViewModel.MAX_HINTS - state.hintsUsed),
                    style = MaterialTheme.typography.titleMedium.scaledBy(QuietButtonScale),
                )
            }
        }
    }
}

@Composable
private fun CompletedContent(state: ChallengeUiState) {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        StageDots(current = state.stages.size, total = state.stages.size)
        Spacer(Modifier.height(28.dp))
        Text(text = "🏆", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        Text(
            text = strings.challengeDoneHeadline,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = strings.challengeDoneBody(state.totalCompleted),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = strings.challengeTomorrow,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** One dot per stage: filled when passed, ringed when still ahead. */
@Composable
private fun StageDots(current: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(total) { index ->
            val passed = index < current
            Box(
                Modifier
                    .size(if (index == current) 14.dp else 12.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            passed -> MaterialTheme.colorScheme.primary
                            index == current -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                    ),
            )
        }
    }
}
