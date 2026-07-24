package com.ak.momapp.ui.challenge

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ak.momapp.data.SettingsRepository
import com.ak.momapp.i18n.LocalStrings
import com.ak.momapp.ui.problem.ConfettiBurst
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
    // While she's typing, the Hint button slides down out of the way.
    val imeVisible = WindowInsets.isImeVisible

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
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
            Spacer(Modifier.height(12.dp))
            Text(
                text = state.intro,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))

            ProblemTextCard(
                text = state.stage.text,
                baseStyle = MaterialTheme.typography.headlineMedium,
                diagram = state.stage.diagram,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = state.input,
                onValueChange = onInputChange,
                enabled = state.phase != ChallengePhase.STAGE_DONE,
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
                suffix = state.stage.answerUnit
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
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(20.dp))

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
            Spacer(Modifier.height(16.dp))
        }

        if (state.phase == ChallengePhase.STAGE_DONE) {
            FilledTonalButton(
                onClick = onNextStage,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
            ) {
                Text(strings.challengeContinue, style = MaterialTheme.typography.titleLarge)
            }
        } else {
            Button(
                onClick = onSubmit,
                enabled = state.input.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
            ) {
                Text(strings.check, style = MaterialTheme.typography.titleLarge)
            }
            // The keyboard slides the Hint button away, keeping the story
            // and hint readable and out of accidental tapping range.
            AnimatedVisibility(
                visible = !imeVisible,
                enter = expandVertically() + slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + shrinkVertically() + fadeOut(),
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Spacer(Modifier.height(8.dp))
                    FilledTonalButton(
                        onClick = onUseHint,
                        enabled = state.hintsUsed < ChallengeViewModel.MAX_HINTS,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                    ) {
                        Text(
                            text = strings.hintButton(ChallengeViewModel.MAX_HINTS - state.hintsUsed),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
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
