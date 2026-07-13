package com.ak.momapp.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ak.momapp.data.BrainBreakStats
import com.ak.momapp.data.DayCount
import com.ak.momapp.data.TopicTally
import java.time.LocalDate
import java.time.format.TextStyle
import com.ak.momapp.i18n.LocalStrings
import com.ak.momapp.problem.ProblemTopic
import com.ak.momapp.ui.theme.MomAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StatsViewModel = viewModel(factory = StatsViewModel.Factory),
) {
    val stats by viewModel.stats.collectAsState()

    val strings = LocalStrings.current
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(strings.statsTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                },
            )
        },
    ) { innerPadding ->
        val current = stats
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
            StatsContent(
                stats = current,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun StatsContent(stats: BrainBreakStats, modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(16.dp))

        Text(
            text = "${stats.solvedThisWeek}",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = strings.problemsThisWeek(stats.solvedThisWeek),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))

        // Two rows of two so the labels stay readable at Large text.
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            StatCard(
                value = "${stats.solvedToday}",
                label = strings.todayCard,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                value = stats.accuracyPercent?.let { "$it%" } ?: "–",
                label = strings.accuracyCard,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            StatCard(
                value = stats.fastestMs?.let(::formatDuration) ?: "–",
                label = strings.fastestCard,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                value = "${stats.solvedAllTime}",
                label = strings.allTimeCard,
                modifier = Modifier.weight(1f),
            )
        }

        if (stats.lastTwoWeeks.any { it.count > 0 }) {
            ActivityChartCard(days = stats.lastTwoWeeks)
        }

        if (stats.topicTallies.isNotEmpty()) {
            TopicBreakdownCard(tallies = stats.topicTallies)
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = when {
                stats.solvedToday > 0 -> strings.statsLineToday
                stats.solvedThisWeek > 0 -> strings.statsLineWeek
                else -> strings.statsLineFreshWeek
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Fourteen little bars, one per day, ending today. One quiet number sits
 * over the best day so the height scale reads; everything else stays
 * unlabeled — the cards above carry the totals.
 */
@Composable
private fun ActivityChartCard(days: List<DayCount>, modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    val max = days.maxOf { it.count }
    // If several days tie for best, the label goes on the most recent.
    val labeledDate = days.last { it.count == max }.date
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = strings.activityChartTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth(),
            ) {
                days.forEach { day ->
                    val isToday = day == days.last()
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        if (day.date == labeledDate && max > 0) {
                            Text(
                                text = "${day.count}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .height(if (day.count == 0) 3.dp else (8 + 64f * day.count / max).dp)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(
                                    if (day.count == 0) {
                                        MaterialTheme.colorScheme.outlineVariant
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                ),
                        )
                        Text(
                            text = day.date.dayOfWeek.getDisplayName(TextStyle.NARROW, strings.locale),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (isToday) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

/**
 * First-try accuracy split by exercise type. Only types that have been
 * seen at least once get a row, in the Exercise-screen order.
 */
@Composable
private fun TopicBreakdownCard(
    tallies: Map<ProblemTopic, TopicTally>,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = strings.topicBreakdownTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            ProblemTopic.entries.forEach { topic ->
                val tally = tallies[topic] ?: return@forEach
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = strings.topicLabel(topic),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = tally.percent?.let { "$it%" } ?: "–",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** "4.2 s" under ten seconds, "23 s" under a minute, then "1 min 5 s". */
private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000.0
    return when {
        totalSeconds < 10 -> "%.1f s".format(totalSeconds)
        totalSeconds < 60 -> "${totalSeconds.toInt()} s"
        else -> "${(totalSeconds / 60).toInt()} min ${(totalSeconds % 60).toInt()} s"
    }
}

@Preview(showBackground = true)
@Composable
private fun StatsPreview() {
    MomAppTheme {
        StatsContent(
            stats = BrainBreakStats(
                solvedToday = 3,
                solvedThisWeek = 14,
                solvedAllTime = 87,
                fastestMs = 4_200,
                lastTwoWeeks = (13 downTo 0).map { back ->
                    DayCount(LocalDate.now().minusDays(back.toLong()), (back * 5) % 7)
                },
            ),
        )
    }
}
