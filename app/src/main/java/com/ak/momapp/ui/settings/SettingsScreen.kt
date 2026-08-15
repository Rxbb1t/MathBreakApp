package com.ak.momapp.ui.settings

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ak.momapp.alarm.BreakScheduler
import com.ak.momapp.data.BrainBreakSettings
import com.ak.momapp.i18n.AppLanguage
import com.ak.momapp.i18n.LocalStrings
import com.ak.momapp.i18n.formatNextBreak
import com.ak.momapp.i18n.formatTimeOfDay
import com.ak.momapp.problem.Difficulty
import com.ak.momapp.problem.ProblemTopic
import com.ak.momapp.ui.PresetDialog
import com.ak.momapp.ui.theme.AppPalette
import com.ak.momapp.ui.theme.UiSkin
import com.ak.momapp.widget.BrainBreakWidgetReceiver
import java.time.DayOfWeek
import java.time.format.TextStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onResetSitting: () -> Unit = {},
    onOpenStats: () -> Unit = {},
    onOpenExercises: () -> Unit = {},
    onOpenErrorReport: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val settings by viewModel.settings.collectAsState()
    val strings = LocalStrings.current
    var showPalettePicker by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(strings.settingsTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                },
                actions = {
                    IconButton(onClick = onOpenStats) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = strings.statsIconDescription,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = { showPalettePicker = true }) {
                        Text(strings.personalize)
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
            SettingsContent(
                settings = current,
                viewModel = viewModel,
                onResetSitting = onResetSitting,
                onOpenExercises = onOpenExercises,
                onOpenErrorReport = onOpenErrorReport,
                modifier = Modifier
                    .padding(innerPadding)
                    .imePadding(),
            )
            if (showPalettePicker) {
                PalettePickerDialog(
                    selected = current.palette,
                    onSelect = viewModel::setPalette,
                    onDismiss = { showPalettePicker = false },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SettingsContent(
    settings: BrainBreakSettings,
    viewModel: SettingsViewModel,
    onResetSitting: () -> Unit,
    onOpenExercises: () -> Unit,
    onOpenErrorReport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    var showStartPicker by rememberSaveable { mutableStateOf(false) }
    var showEndPicker by rememberSaveable { mutableStateOf(false) }
    var showPresets by rememberSaveable { mutableStateOf(false) }

    if (showPresets) {
        PresetDialog(
            onPick = viewModel::applyPreset,
            onDismiss = { showPresets = false },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Three questions, in the order somebody actually asks them: when
        // does the app interrupt me, what happens when it does, and how
        // does the app itself behave. The cards inside each group are
        // unchanged; only their company is.
        SettingsGroup(strings.settingsGroupBreaks)

        RemindersSection(settings = settings, viewModel = viewModel)

        SettingsSection(title = strings.remindMeEvery) {
            // Chips instead of a segmented row: they wrap on narrow
            // screens or at a large system font scale.
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val options = BrainBreakSettings.MIN_INTERVAL_HOURS..BrainBreakSettings.MAX_INTERVAL_HOURS
                options.forEach { hours ->
                    FilterChip(
                        selected = settings.intervalHours == hours,
                        onClick = { viewModel.setIntervalHours(hours) },
                        label = { Text(strings.hourOption(hours)) },
                    )
                }
            }
        }

        SettingsSection(
            title = strings.activeHours,
            subtitle = strings.activeHoursSubtitle,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedButton(
                    onClick = { showStartPicker = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                ) {
                    Text(strings.fromTime(formatTimeOfDay(settings.activeStartMinutes)))
                }
                OutlinedButton(
                    onClick = { showEndPicker = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                ) {
                    Text(strings.untilTime(formatTimeOfDay(settings.activeEndMinutes)))
                }
            }
        }

        SettingsSection(title = strings.activeDays) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                DayOfWeek.entries.forEach { day ->
                    FilterChip(
                        selected = day in settings.activeDays,
                        onClick = { viewModel.toggleDay(day) },
                        label = { Text(day.getDisplayName(TextStyle.SHORT, strings.locale)) },
                    )
                }
            }
        }

        SettingsGroup(strings.settingsGroupExercises)

        SettingsSection(
            title = strings.startingLevel,
            subtitle = strings.startingLevelSubtitle,
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Difficulty.entries.forEach { difficulty ->
                    FilterChip(
                        selected = settings.startingDifficulty == difficulty,
                        onClick = { viewModel.setStartingDifficulty(difficulty) },
                        label = { Text(strings.difficultyLabel(difficulty)) },
                    )
                }
            }
            // What the picked level actually deals; follows the selection.
            Text(
                text = strings.difficultyExplanation(settings.startingDifficulty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Sits with the level rather than with the per-break cap, which
            // is where it used to be: what this button really does is put
            // the level back to the one picked just above, so it belongs
            // next to that choice and nowhere else.
            FilledTonalButton(onClick = onResetSitting, modifier = Modifier.fillMaxWidth()) {
                Text(strings.resetSittingButton)
            }
            // The same three presets the first-run guide offered. A choice
            // made before answering a single problem deserves an easy way
            // back: one tap here rather than four settings to undo.
            FilledTonalButton(
                onClick = { showPresets = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(strings.quickSetupAction)
            }
        }

        ExerciseTypesRow(settings = settings, onClick = onOpenExercises)

        SettingsSection(
            title = strings.problemsPerBreak,
            subtitle = strings.problemsPerBreakSubtitle,
        ) {
            ProblemLimitChips(
                current = settings.problemsPerBreak,
                onSelect = viewModel::setProblemsPerBreak,
            )
        }

        SettingsGroup(strings.settingsGroupApp)

        SettingsSection(title = strings.languageSection) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                AppLanguage.entries.forEachIndexed { index, language ->
                    SegmentedButton(
                        selected = settings.language == language,
                        onClick = { viewModel.setLanguage(language) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = AppLanguage.entries.size,
                        ),
                    ) {
                        Text(if (language == AppLanguage.ENGLISH) "English" else "Română")
                    }
                }
            }
        }

        SettingsSection(
            title = strings.appearanceTitle,
            subtitle = strings.appearanceSubtitle,
        ) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                UiSkin.entries.forEachIndexed { index, skin ->
                    SegmentedButton(
                        selected = settings.skin == skin,
                        onClick = { viewModel.setSkin(skin) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = UiSkin.entries.size,
                        ),
                    ) {
                        Text(
                            if (skin == UiSkin.LEGACY) {
                                strings.appearanceLegacy
                            } else {
                                strings.appearanceModern
                            },
                        )
                    }
                }
            }
        }

        SettingsSection(
            title = strings.soundSection,
            subtitle = strings.successSoundSubtitle,
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = strings.successSoundLabel,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Switch(
                    checked = settings.successSound,
                    onCheckedChange = viewModel::setSuccessSound,
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        LinkRow(label = strings.errorReportAction, onClick = onOpenErrorReport)

        // Hidden until there is somewhere to send her. An empty link would
        // be worse than no link.
        if (SupportLink.URL.isNotBlank()) {
            val context = LocalContext.current
            Spacer(Modifier.height(8.dp))
            LinkRow(
                label = strings.supportAction,
                onClick = { openSupportPage(context) },
            )
        }

        Spacer(Modifier.height(8.dp))
    }

    if (showStartPicker) {
        TimePickerDialog(
            title = strings.activeFromTitle,
            initialMinutes = settings.activeStartMinutes,
            onConfirm = { minutes ->
                viewModel.setActiveStart(minutes)
                showStartPicker = false
            },
            onDismiss = { showStartPicker = false },
        )
    }
    if (showEndPicker) {
        TimePickerDialog(
            title = strings.activeUntilTitle,
            initialMinutes = settings.activeEndMinutes,
            onConfirm = { minutes ->
                viewModel.setActiveEnd(minutes)
                showEndPicker = false
            },
            onDismiss = { showEndPicker = false },
        )
    }
}

/**
 * One tappable summary row. The full picker with its grouped switches
 * lives on the Exercise types screen, so ten switches don't pile up here.
 */
/**
 * Where a tip would go.
 *
 * Left blank deliberately: the account does not exist yet, and the row
 * above hides itself while this is empty, so filling in one string is the
 * whole job when it does.
 *
 * IT MUST STAY A PLAIN LINK OUT TO A BROWSER. A donation is only allowed
 * to sit outside the Play billing rules while it buys nothing, so nothing
 * in the app may ever unlock, change, or improve because someone followed
 * it. Putting the payment page in a WebView would also blur exactly the
 * line that keeps this simple.
 */
private object SupportLink {
    const val URL = ""
}

private fun openSupportPage(context: Context) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SupportLink.URL)))
    }
}

/** A tappable row that just leads somewhere, with no state of its own. */
@Composable
private fun LinkRow(label: String, onClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ExerciseTypesRow(
    settings: BrainBreakSettings,
    onClick: () -> Unit,
) {
    val strings = LocalStrings.current
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = strings.exerciseTypesTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = strings.exerciseTypesSummary(
                        settings.enabledTopics.size,
                        ProblemTopic.entries.size,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun RemindersSection(
    settings: BrainBreakSettings,
    viewModel: SettingsViewModel,
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val nextBreakAt by viewModel.nextBreakAt.collectAsState()

    // Both checks are re-run when she comes back from system settings.
    var permissionRefresh by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        permissionRefresh++
        onPauseOrDispose {}
    }
    val notificationsAllowed = remember(permissionRefresh) {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    val exactAlarmsAllowed = remember(permissionRefresh) {
        BreakScheduler.canScheduleExact(context)
    }

    SettingsSection(title = strings.remindersSection) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = strings.breakReminders,
                style = MaterialTheme.typography.bodyLarge,
            )
            Switch(
                checked = settings.remindersEnabled,
                onCheckedChange = viewModel::setRemindersEnabled,
            )
        }

        if (settings.remindersEnabled) {
            when {
                !notificationsAllowed -> PermissionHint(
                    text = strings.notificationsOffHint,
                    buttonLabel = strings.turnOnNotifications,
                ) {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                    )
                }

                !exactAlarmsAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> PermissionHint(
                    text = strings.exactAlarmHint,
                    buttonLabel = strings.allowExactTiming,
                ) {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                            .setData(Uri.fromParts("package", context.packageName, null)),
                    )
                }
            }

            nextBreakAt?.let { at ->
                Text(
                    text = strings.nextReminder(formatNextBreak(at, strings)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        AddWidgetButton()
    }
}

/**
 * Offers to drop the widget on the home screen from inside the app. The
 * system widget picker is a long-press on empty wallpaper followed by a
 * scroll through every app on the phone -- not somewhere she would ever
 * find it. Hidden entirely on launchers that can't pin.
 */
@Composable
private fun AddWidgetButton() {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val manager = remember { AppWidgetManager.getInstance(context) }
    if (!manager.isRequestPinAppWidgetSupported) return

    OutlinedButton(
        onClick = {
            manager.requestPinAppWidget(
                ComponentName(context, BrainBreakWidgetReceiver::class.java),
                null,
                null,
            )
        },
    ) {
        Text(strings.addWidget)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProblemLimitChips(
    current: Int,
    onSelect: (Int) -> Unit,
) {
    val strings = LocalStrings.current
    var showCustomDialog by rememberSaveable { mutableStateOf(false) }
    val isPreset = current == BrainBreakSettings.UNLIMITED_PROBLEMS ||
        current in BrainBreakSettings.PROBLEM_LIMIT_PRESETS

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BrainBreakSettings.PROBLEM_LIMIT_PRESETS.forEach { preset ->
            FilterChip(
                selected = current == preset,
                onClick = { onSelect(preset) },
                label = { Text("$preset") },
            )
        }
        FilterChip(
            selected = current == BrainBreakSettings.UNLIMITED_PROBLEMS,
            onClick = { onSelect(BrainBreakSettings.UNLIMITED_PROBLEMS) },
            label = { Text(strings.noLimit) },
        )
        FilterChip(
            selected = !isPreset,
            onClick = { showCustomDialog = true },
            label = { Text(if (isPreset) strings.customEllipsis else strings.customValue(current)) },
        )
    }

    if (showCustomDialog) {
        CustomLimitDialog(
            initial = if (isPreset) null else current,
            onConfirm = { count ->
                onSelect(count)
                showCustomDialog = false
            },
            onDismiss = { showCustomDialog = false },
        )
    }
}

@Composable
private fun CustomLimitDialog(
    initial: Int?,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    var input by rememberSaveable { mutableStateOf(initial?.toString() ?: "") }
    val parsed = input.toIntOrNull()
        ?.coerceIn(1, BrainBreakSettings.MAX_PROBLEMS_PER_BREAK)

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.problemsPerBreak) },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { value -> input = value.filter(Char::isDigit).take(2) },
                singleLine = true,
                placeholder = { Text(strings.customPlaceholder) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { parsed?.let(onConfirm) }),
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = { parsed?.let(onConfirm) },
                enabled = parsed != null,
            ) {
                Text(strings.ok)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        },
    )
}

@Composable
private fun PermissionHint(
    text: String,
    buttonLabel: String,
    onClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.tertiary,
        )
        FilledTonalButton(onClick = onClick) {
            Text(buttonLabel)
        }
    }
}

/**
 * The Personalize menu: five palettes, applied (and saved) on tap so the
 * whole app re-tints behind the dialog for an instant preview.
 */
@Composable
private fun PalettePickerDialog(
    selected: AppPalette,
    onSelect: (AppPalette) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.personalizeTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppPalette.entries.forEach { palette ->
                    val isSelected = palette == selected
                    Surface(
                        onClick = { onSelect(palette) },
                        shape = MaterialTheme.shapes.medium,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            Color.Transparent
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        ) {
                            palette.swatch.forEach { accent ->
                                Box(
                                    Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(accent),
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = strings.paletteName(palette),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                            if (isSelected) {
                                Icon(Icons.Filled.Check, contentDescription = null)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.ok)
            }
        },
    )
}

/**
 * A heading over the cards that follow it.
 *
 * Deliberately NOT a container the sections nest inside. The screen is one
 * long scroll, and a card of cards gains a second border and a second
 * inset without telling her anything the heading does not. This just
 * labels where one group starts, the way a heading in a document does.
 */
@Composable
private fun SettingsGroup(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
    )
}

@Composable
private fun SettingsSection(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit,
) {
    // Each section sits on its own soft, warm card.
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    title: String,
    initialMinutes: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    val state = rememberTimePickerState(
        initialHour = initialMinutes / 60,
        initialMinute = initialMinutes % 60,
        is24Hour = true,
    )
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { TimePicker(state) },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = { onConfirm(state.hour * 60 + state.minute) },
            ) {
                Text(strings.ok)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        },
    )
}
