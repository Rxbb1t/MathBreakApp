package com.ak.momapp.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.ak.momapp.data.CrashLog
import com.ak.momapp.i18n.LocalStrings
import com.ak.momapp.ui.sectionSurfaceBorder
import com.ak.momapp.ui.sectionSurfaceColor

/**
 * What went wrong last time, and a button that puts it on the clipboard.
 *
 * This app has no analytics and no network permission, which is the right
 * trade for something this personal but leaves a real gap: a crash on
 * someone else's phone is invisible forever. The whole reporting pipeline
 * is therefore manual and offline. She copies, and pastes it wherever she
 * likes.
 *
 * The device and version lines are shown even when nothing has crashed,
 * because plenty of problems are not crashes and those two facts are the
 * first thing anyone would ask about a report of any kind.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorReportScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    // Read once. The file only changes when the process dies, so there is
    // nothing here that could go stale while she is looking at it.
    var report by remember { mutableStateOf(CrashLog.read(context)) }
    val device = remember { CrashLog.describeDevice(context) }
    var copied by remember { mutableStateOf(false) }

    Surface(modifier = modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(strings.errorReportTitle) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                        }
                    },
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 20.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = if (report == null) strings.errorReportNoneBody else strings.errorReportBody,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(20.dp))

                DetailCard(device)

                report?.let { text ->
                    Spacer(Modifier.height(12.dp))
                    DetailCard(text)
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = {
                            copyToClipboard(context, text)
                            copied = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (copied) strings.errorReportCopied else strings.errorReportCopy)
                    }
                    Spacer(Modifier.height(8.dp))
                    FilledTonalButton(
                        onClick = {
                            CrashLog.clear(context)
                            report = null
                            copied = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(strings.errorReportClear)
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

/**
 * A stack trace has long lines that must not be allowed to stretch the
 * page, so it scrolls sideways inside its own card instead.
 */
@Composable
private fun DetailCard(text: String) {
    Surface(
        color = sectionSurfaceColor(),
        border = sectionSurfaceBorder(),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .horizontalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("Brain Break error", text))
}
