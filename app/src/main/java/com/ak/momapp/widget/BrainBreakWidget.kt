package com.ak.momapp.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.ak.momapp.MainActivity
import com.ak.momapp.data.BreakStateRepository
import com.ak.momapp.data.ProgressRepository
import com.ak.momapp.data.SettingsRepository
import com.ak.momapp.i18n.AppLanguage
import com.ak.momapp.i18n.formatNextBreak
import com.ak.momapp.i18n.strings
import com.ak.momapp.ui.theme.AppPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Everything the widget draws, gathered from the three repositories that
 * own it. Collapsing them into one value up front means the widget body
 * never has to reason about a half-loaded state.
 */
private data class WidgetState(
    val language: AppLanguage = AppLanguage.ENGLISH,
    val palette: AppPalette = AppPalette.CLAY,
    val solvedToday: Int = 0,
    /** Null when reminders are off or nothing is scheduled. */
    val nextBreakAt: Long? = null,
)

private fun widgetState(context: Context): Flow<WidgetState> = combine(
    SettingsRepository(context).settings,
    ProgressRepository(context).stats,
    BreakStateRepository(context).nextBreakAt,
) { settings, stats, nextBreakAt ->
    WidgetState(
        language = settings.language,
        palette = settings.palette,
        solvedToday = stats.solvedToday,
        // A stale time left over from before she switched reminders off
        // would promise a break that is never coming.
        nextBreakAt = nextBreakAt.takeIf { settings.remindersEnabled },
    )
}

/**
 * A home-screen glance at the day: how many she has solved, when the next
 * break is due, and a tap to start one early.
 *
 * The widget is deliberately read-only apart from that tap. It is a
 * reminder that lives where she already looks, not a second place to do
 * the exercises.
 */
class BrainBreakWidget : GlanceAppWidget() {

    // Nothing here depends on the size: the three lines are ordered most
    // important first, so a widget too short for all of them loses the
    // invitation and keeps the two facts. One composition is enough.
    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = widgetState(context.applicationContext)
        // Read once before composing so the first frame is the real
        // numbers rather than a zero that corrects itself a beat later.
        val initial = state.first()
        provideContent {
            val current by state.collectAsState(initial)
            WidgetBody(current)
        }
    }

    companion object {
        /**
         * Redraws every placed widget. Called from the two places its
         * numbers change: a recorded solve and a rescheduled break.
         *
         * Both callers are on the main thread -- app startup and the
         * answer handler -- and updateAll composes the widget and
         * translates it to RemoteViews on whatever dispatcher it is
         * handed. That is far too much work to put in front of a frame,
         * so it is moved off explicitly here rather than at each call.
         */
        suspend fun refresh(context: Context) = withContext(Dispatchers.Default) {
            BrainBreakWidget().updateAll(context.applicationContext)
        }
    }
}

@Composable
private fun WidgetBody(state: WidgetState) {
    val strings = state.language.strings()
    // The widget host has no notion of the app's light/dark choice, so
    // the palette supplies both and Glance picks by system theme.
    val colors = ColorProviders(
        light = state.palette.colors(darkTheme = false),
        dark = state.palette.colors(darkTheme = true),
    )
    GlanceTheme(colors = colors) {
        val openProblem = startProblemIntent(LocalContext.current)
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surfaceVariant)
                .appWidgetBackground()
                .cornerRadius(20.dp)
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .clickable(actionStartActivity(openProblem)),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(
                text = strings.solvedToday(state.solvedToday),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 1,
            )
            Text(
                text = state.nextBreakAt
                    ?.let { strings.nextReminder(formatNextBreak(it, strings)) }
                    ?: strings.widgetRemindersOff,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 13.sp,
                ),
                maxLines = 1,
            )
            Text(
                text = strings.widgetTapPrompt,
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 1,
            )
        }
    }
}

/**
 * Lands on a freshly dealt problem instead of the Start screen. The tap
 * was the start -- the same reasoning as a break notification, minus the
 * snooze: nothing nudged her, so there is nothing to put off.
 */
private fun startProblemIntent(context: Context) =
    Intent(context, MainActivity::class.java)
        .putExtra(MainActivity.EXTRA_START_PROBLEM, true)

class BrainBreakWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BrainBreakWidget()
}
