package com.ak.momapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.ak.momapp.alarm.BreakCoordinator
import com.ak.momapp.ui.AppRoot
import com.ak.momapp.ui.problem.Chimes
import kotlinx.coroutines.launch

/**
 * A "start a problem now" request that arrived from outside the app.
 *
 * [session] counts them so the problem screen can tell one arrival from
 * the next; 0 means she opened the app herself and should get the Start
 * screen as usual.
 */
data class BreakEntry(
    val session: Int = 0,
    /**
     * True only for a break notification. The widget and the launcher
     * icon have nothing to snooze -- nothing nudged her.
     */
    val snoozable: Boolean = false,
)

class MainActivity : ComponentActivity() {

    private var entry by mutableStateOf(BreakEntry())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        entry = entryFor(intent, previous = BreakEntry())
        enableEdgeToEdge()
        setContent {
            // AppRoot applies MomAppTheme itself. It needs the persisted
            // text-size choice, which lives with the other settings.
            AppRoot(
                entry = entry,
                onSnooze = ::snoozeBreak,
            )
        }
        // Self-heals the alarm chain: first launch, cleared app data, or an
        // alarm the system dropped.
        lifecycleScope.launch { BreakCoordinator.ensureScheduled(applicationContext) }
        // Render + load the feedback sounds ahead of the first play.
        Chimes.preload(applicationContext)
    }

    // launchMode=singleTop: a notification or widget tap while the app is
    // already open lands here instead of onCreate.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        entry = entryFor(intent, previous = entry)
    }

    /**
     * Returns the entry [intent] asks for, or [previous] unchanged if it
     * asks for nothing -- a plain launcher tap must not restart a sitting.
     */
    private fun entryFor(intent: Intent?, previous: BreakEntry): BreakEntry = when {
        intent == null -> previous
        intent.getBooleanExtra(EXTRA_FROM_BREAK, false) ->
            BreakEntry(session = previous.session + 1, snoozable = true)
        intent.getBooleanExtra(EXTRA_START_PROBLEM, false) ->
            BreakEntry(session = previous.session + 1, snoozable = false)
        else -> previous
    }

    private fun snoozeBreak() {
        entry = BreakEntry()
        lifecycleScope.launch {
            BreakCoordinator.snooze(applicationContext)
            moveTaskToBack(true)
        }
    }

    companion object {
        const val EXTRA_FROM_BREAK = "from_break"

        /** Start a problem, but this was her idea -- see [BreakEntry.snoozable]. */
        const val EXTRA_START_PROBLEM = "start_problem"
    }
}
