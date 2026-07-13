package com.ak.momapp.ui.problem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ak.momapp.i18n.LocalStrings

// Paper is paper: the helper sheet keeps its warm ruled-sheet look in
// every palette and in dark mode, like the real thing under a lamp.
internal val NotebookPaper = Color(0xFFFEF8EA)
private val RuleColor = Color(0x554A90D9)
private val MarginColor = Color(0x66C25450)
private val InkColor = Color(0xFF34405C)

/**
 * The helper sheet for the tougher levels: the definitions, rules and
 * theorems that fit the current problem, written on ruled paper.
 * The [notes] arrive frozen in the problem's generation language.
 */
@Composable
fun NotebookPad(
    notes: List<String>,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current

    Box(modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) { drawRuledPaper() }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 40.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        ) {
            Text(
                text = strings.notebookTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = InkColor,
            )
            Text(
                text = strings.notebookSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = InkColor.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(16.dp))
            notes.forEach { note ->
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodyLarge,
                    color = InkColor,
                    modifier = Modifier.padding(bottom = 14.dp),
                )
            }
        }
    }
}

private fun DrawScope.drawRuledPaper() {
    val gap = 34.dp.toPx()
    var y = gap
    while (y < size.height) {
        drawLine(RuleColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        y += gap
    }
    val marginX = 28.dp.toPx()
    drawLine(MarginColor, Offset(marginX, 0f), Offset(marginX, size.height), strokeWidth = 1.5.dp.toPx())
}
