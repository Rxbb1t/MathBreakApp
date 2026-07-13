package com.ak.momapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ak.momapp.data.SetupPreset
import com.ak.momapp.i18n.LocalStrings

/**
 * First-open setup: one warm question and three preset cards (Balanced,
 * Relaxed, Challenge). Picking one applies its settings and dismisses
 * the guide for good; there is no other way out, so the choice is
 * always deliberate. Everything a preset sets can be changed later in
 * Settings.
 */
@Composable
fun SetupGuideDialog(onPick: (SetupPreset) -> Unit) {
    val strings = LocalStrings.current
    AlertDialog(
        // No dismiss without choosing. A stray outside tap would
        // otherwise leave every setting at its bare default silently.
        onDismissRequest = {},
        title = { Text(strings.guideTitle) },
        text = {
            // Scrolls when Large text meets a short screen.
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                Text(strings.guideIntro, style = MaterialTheme.typography.bodyMedium)
                SetupPreset.entries.forEach { preset ->
                    PresetCard(preset = preset, onClick = { onPick(preset) })
                }
            }
        },
        confirmButton = {},
    )
}

@Composable
private fun PresetCard(preset: SetupPreset, onClick: () -> Unit) {
    val strings = LocalStrings.current
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(
                text = strings.presetTitle(preset),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = strings.presetBody(preset),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}
