package com.ak.momapp.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ak.momapp.data.SettingsRepository
import com.ak.momapp.i18n.AppLanguage
import com.ak.momapp.i18n.LocalStrings
import com.ak.momapp.i18n.strings
import com.ak.momapp.ui.challenge.ChallengeScreen
import com.ak.momapp.ui.exercises.ExercisesScreen
import com.ak.momapp.ui.problem.ProblemScreen
import com.ak.momapp.ui.problem.ProblemViewModel
import com.ak.momapp.ui.settings.SettingsScreen
import com.ak.momapp.ui.stats.StatsScreen
import com.ak.momapp.ui.theme.AppPalette
import com.ak.momapp.ui.theme.MomAppTheme
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// Five screens still don't warrant a navigation library.
enum class AppScreen { PROBLEM, CHALLENGE, SETTINGS, STATS, EXERCISES }

@Composable
fun AppRoot(
    breakSession: Int = 0,
    onSnooze: () -> Unit = {},
) {
    var screen by rememberSaveable { mutableStateOf(AppScreen.PROBLEM) }

    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context.applicationContext) }
    val appearance by remember {
        settingsRepository.settings.map { Triple(it.language, it.largeText, it.palette) }
    }.collectAsState(initial = Triple(AppLanguage.ENGLISH, false, AppPalette.CLAY))
    val (language, largeText, palette) = appearance
    // Null while the first read is in flight, so neither the guide nor the
    // notification prompt can fire before the stored value is known.
    val guideShown by remember {
        settingsRepository.settings.map { it.guideShown as Boolean? }
    }.collectAsState(initial = null)
    val scope = rememberCoroutineScope()

    // Activity-scoped, so it survives trips to settings: the reset-round
    // button there acts on the same sitting the problem screen shows.
    val problemViewModel: ProblemViewModel = viewModel(factory = ProblemViewModel.Factory)

    // The system permission dialog waits its turn: it only appears once
    // the setup guide has been answered, never on top of it.
    NotificationPermissionRequest(enabled = guideShown == true)

    MomAppTheme(largeText = largeText, palette = palette) {
        CompositionLocalProvider(LocalStrings provides language.strings()) {
            when (screen) {
                AppScreen.PROBLEM -> ProblemScreen(
                    onOpenSettings = { screen = AppScreen.SETTINGS },
                    onOpenChallenge = { screen = AppScreen.CHALLENGE },
                    breakSession = breakSession,
                    onSnooze = onSnooze,
                    viewModel = problemViewModel,
                )

                AppScreen.CHALLENGE -> {
                    BackHandler { screen = AppScreen.PROBLEM }
                    ChallengeScreen(onBack = { screen = AppScreen.PROBLEM })
                }

                AppScreen.SETTINGS -> {
                    BackHandler { screen = AppScreen.PROBLEM }
                    SettingsScreen(
                        onBack = { screen = AppScreen.PROBLEM },
                        // Straight into the freshly dealt problem. Staying
                        // in settings made the button feel like a no-op.
                        onResetSitting = {
                            problemViewModel.resetSitting()
                            screen = AppScreen.PROBLEM
                        },
                        onOpenStats = { screen = AppScreen.STATS },
                        onOpenExercises = { screen = AppScreen.EXERCISES },
                    )
                }

                // Stats is reached through Settings now, so back returns there.
                AppScreen.STATS -> {
                    BackHandler { screen = AppScreen.SETTINGS }
                    StatsScreen(onBack = { screen = AppScreen.SETTINGS })
                }

                // Same for the exercise-type picker.
                AppScreen.EXERCISES -> {
                    BackHandler { screen = AppScreen.SETTINGS }
                    ExercisesScreen(onBack = { screen = AppScreen.SETTINGS })
                }
            }

            if (guideShown == false) {
                SetupGuideDialog(
                    onPick = { preset -> scope.launch { settingsRepository.applyPreset(preset) } },
                )
            }
        }
    }
}

/**
 * Asks for POST_NOTIFICATIONS once per app start on Android 13+, but only
 * after [enabled] flips true (the setup guide has been answered). If she
 * declines twice the system stops showing the dialog, and the settings
 * screen takes over with a pointer to system settings.
 */
@Composable
private fun NotificationPermissionRequest(enabled: Boolean) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {}
    LaunchedEffect(enabled) {
        if (!enabled) return@LaunchedEffect
        val granted = context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
