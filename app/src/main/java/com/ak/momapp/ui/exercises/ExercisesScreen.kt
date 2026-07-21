package com.ak.momapp.ui.exercises

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ak.momapp.i18n.LocalStrings
import com.ak.momapp.problem.Difficulty
import com.ak.momapp.problem.Level
import com.ak.momapp.problem.ProblemTopic
import com.ak.momapp.problem.TopicGroup
import com.ak.momapp.ui.problem.LevelProgressReveal
import com.ak.momapp.ui.settings.SettingsViewModel

/**
 * The full exercise-type picker, one switch per topic grouped under
 * Numbers / Everyday stories / Thinking headers, each with a one-line
 * description. Settings shows only a summary row that leads here. When
 * just [ProblemTopic.MIN_ENABLED] switches are left on, those lock
 * until another one joins them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val settings by viewModel.settings.collectAsState()
    val topicLevels by viewModel.topicLevels.collectAsState()
    val topicPoints by viewModel.topicPoints.collectAsState()
    val strings = LocalStrings.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(strings.exerciseTypesTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                },
            )
        },
    ) { innerPadding ->
        val current = settings
        if (current == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = strings.problemTypesSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                Text(
                    text = strings.topicLevelsHint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                val enabledTopics = current.enabledTopics
                TopicGroup.entries.forEach { group ->
                    TopicGroupCard(
                        group = group,
                        enabledTopics = enabledTopics,
                        topicLevels = topicLevels,
                        topicPoints = topicPoints,
                        onToggle = viewModel::toggleTopic,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TopicGroupCard(
    group: TopicGroup,
    enabledTopics: Set<ProblemTopic>,
    topicLevels: Map<ProblemTopic, Difficulty>,
    topicPoints: Map<ProblemTopic, Level>,
    onToggle: (ProblemTopic) -> Unit,
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
                val isOn = topic in enabledTopics
                val level = topicLevels[topic]
                // The last topic standing can't be switched off.
                val canToggle = !isOn || enabledTopics.size > ProblemTopic.MIN_ENABLED
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        // The whole row is the target, not just the switch.
                        .clickable(enabled = canToggle) { onToggle(topic) },
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = strings.topicLabel(topic),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = strings.topicDescription(topic),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        // The level is meaningless for a topic that never
                        // gets dealt, so it goes quiet with the switch.
                        if (isOn && level != null) {
                            var showDetail by remember { mutableStateOf(false) }
                            Text(
                                text = strings.difficultyLabel(level),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                // Long-press the level to see where this
                                // topic actually sits. Tap still belongs to
                                // the row, so the switch keeps its big
                                // target and nothing about the gesture is
                                // advertised.
                                modifier = Modifier.combinedClickable(
                                    onClick = { if (canToggle) onToggle(topic) },
                                    onLongClick = { showDetail = !showDetail },
                                ),
                            )
                            LevelProgressReveal(
                                visible = showDetail,
                                level = topicPoints[topic],
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = isOn,
                        enabled = canToggle,
                        onCheckedChange = { onToggle(topic) },
                    )
                }
            }
        }
    }
}
