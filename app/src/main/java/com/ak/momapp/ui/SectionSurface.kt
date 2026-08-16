package com.ak.momapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ak.momapp.ui.theme.LocalSkin
import com.ak.momapp.ui.theme.UiSkin

/**
 * The surface every grouped section sits on, shared by Settings,
 * Exercises, the setup guide and the error report.
 *
 * Those four drew the same card three slightly different ways before
 * this. The treatment is exposed as colour and border rather than as one
 * wrapper composable because some of these cards are tappable Surfaces
 * with their own onClick and some are plain containers; sharing the look
 * without forcing a shared structure is what suits them.
 *
 * Legacy's 40%-alpha wash muddies at large text, where more of the screen
 * is card and less is background, and two cards touching become one
 * shape. Modern gives it a real container and a hairline instead.
 */
@Composable
@ReadOnlyComposable
fun sectionSurfaceColor(): Color =
    if (LocalSkin.current == UiSkin.MODERN) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    }

/** The hairline that separates one Modern card from the next. Legacy has none. */
@Composable
@ReadOnlyComposable
fun sectionSurfaceBorder(): BorderStroke? =
    if (LocalSkin.current == UiSkin.MODERN) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    } else {
        null
    }

/** A plain grouped card, for the sections that are not themselves tappable. */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = sectionSurfaceColor(),
        border = sectionSurfaceBorder(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}
