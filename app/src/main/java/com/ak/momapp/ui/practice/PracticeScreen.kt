package com.ak.momapp.ui.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ak.momapp.i18n.LocalStrings
import com.ak.momapp.problem.Difficulty
import com.ak.momapp.problem.ProblemTopic
import com.ak.momapp.problem.TopicGroup
import com.ak.momapp.ui.problem.ProblemScreenContent
import com.ak.momapp.ui.problem.ProblemViewModel

/**
 * Drilling one exercise type on purpose, as opposed to the mixed break.
 * She picks a type and a level; problems then keep coming with no timer
 * and no per-break cap. They still count toward the day's total and the
 * per-topic accuracy, but they leave the adaptive break level alone: the
 * point is to be able to practise a hard type without paying for it
 * later.
 *
 * The drill runs on its own [ProblemViewModel] instance (keyed
 * "practice"), so whatever break problem was on screen is still there
 * when she comes back.
 */
@Composable
fun PracticeScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProblemViewModel = viewModel(
        key = PRACTICE_VM_KEY,
        factory = ProblemViewModel.Factory,
    ),
) {
    val practice by viewModel.practice.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val solvedToday by viewModel.solvedToday.collectAsState()
    val soundEnabled by viewModel.successSound.collectAsState()

    val drill = practice
    if (drill == null) {
        PracticePicker(
            onBack = onBack,
            onPick = viewModel::startPractice,
            modifier = modifier,
        )
        return
    }

    val state = uiState
    if (state == null) {
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
            onOpenSettings = {},
            soundEnabled = soundEnabled,
            practiceTopic = drill.topic,
            onExitPractice = viewModel::endPractice,
            modifier = modifier,
        )
    }
}

/** The type and level picker that opens practice. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PracticePicker(
    onBack: () -> Unit,
    onPick: (ProblemTopic, Difficulty) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    var level by rememberSaveable { mutableStateOf(Difficulty.EASY) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(strings.practiceTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = strings.practiceIntro,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )

            // The level rides above the types: one choice for whichever
            // type she taps next, so picking a type is the last step.
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = strings.practiceLevel,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Difficulty.entries.forEach { option ->
                            FilterChip(
                                selected = option == level,
                                onClick = { level = option },
                                label = { Text(strings.difficultyLabel(option)) },
                            )
                        }
                    }
                }
            }

            TopicGroup.entries.forEach { group ->
                PracticeGroupCard(group = group, onPick = { onPick(it, level) })
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * One card per group, a button per type. Every type is offered here,
 * including the ones switched off for breaks: this screen is a
 * deliberate choice, not the mix.
 */
@Composable
private fun PracticeGroupCard(
    group: TopicGroup,
    onPick: (ProblemTopic) -> Unit,
) {
    val strings = LocalStrings.current
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = strings.topicGroupLabel(group),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            ProblemTopic.entries.filter { it.group == group }.forEach { topic ->
                FilledTonalButton(
                    onClick = { onPick(topic) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = strings.topicLabel(topic),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/** Keeps the drill's view model apart from the break's. */
private const val PRACTICE_VM_KEY = "practice"
