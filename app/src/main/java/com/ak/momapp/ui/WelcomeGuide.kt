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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ak.momapp.data.SetupPreset
import com.ak.momapp.i18n.LocalStrings

/**
 * First-open setup: one warm question and three preset cards (Balanced,
 * Relaxed, Challenge). Picking one applies its settings and dismisses the
 * guide for good; there is no other way out, so the choice is always
 * deliberate. Everything a preset sets can be changed later in Settings,
 * and the whole picker can be reopened there via [PresetDialog].
 */
@Composable
fun SetupGuideDialog(onPick: (SetupPreset) -> Unit) {
    val strings = LocalStrings.current
    AlertDialog(
        // No dismiss without choosing. A stray outside tap would
        // otherwise leave every setting at its bare default silently.
        onDismissRequest = {},
        title = { Text(strings.guideTitle) },
        text = { PresetList(intro = strings.guideIntro, onPick = onPick) },
        confirmButton = {},
    )
}

/**
 * The same three presets, offered again from Settings.
 *
 * The reason this exists: a preset picked on the very first screen is a
 * guess about someone who has not answered a single problem yet. Finding
 * out a week later that Relaxed is too gentle should not mean hunting
 * through four separate settings to undo it. Here it is one tap, in the
 * same words she chose from the first time.
 *
 * Unlike the first-run guide this one CAN be dismissed: she already has
 * working settings, so backing out has to leave them alone.
 */
@Composable
fun PresetDialog(onPick: (SetupPreset) -> Unit, onDismiss: () -> Unit) {
    val strings = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.quickSetupTitle) },
        text = {
            PresetList(
                intro = strings.quickSetupIntro,
                onPick = {
                    onPick(it)
                    onDismiss()
                },
            )
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        },
    )
}

@Composable
private fun PresetList(intro: String, onPick: (SetupPreset) -> Unit) {
    // Scrolls when Large text meets a short screen.
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.verticalScroll(rememberScrollState()),
    ) {
        Text(intro, style = MaterialTheme.typography.bodyMedium)
        SetupPreset.entries.forEach { preset ->
            PresetCard(preset = preset, onClick = { onPick(preset) })
        }
    }
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
