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
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // 0 = not opened from a break; each break notification tap increments,
    // so the problem screen can tell a fresh break from the previous one.
    private var breakSession by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        breakSession = if (intent?.getBooleanExtra(EXTRA_FROM_BREAK, false) == true) 1 else 0
        enableEdgeToEdge()
        setContent {
            // AppRoot applies MomAppTheme itself — it needs the persisted
            // text-size choice, which lives with the other settings.
            AppRoot(
                breakSession = breakSession,
                onSnooze = ::snoozeBreak,
            )
        }
        // Self-heals the alarm chain: first launch, cleared app data, or an
        // alarm the system dropped.
        lifecycleScope.launch { BreakCoordinator.ensureScheduled(applicationContext) }
    }

    // launchMode=singleTop: a notification tap while the app is open lands
    // here instead of onCreate.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_FROM_BREAK, false)) breakSession++
    }

    private fun snoozeBreak() {
        breakSession = 0
        lifecycleScope.launch {
            BreakCoordinator.snooze(applicationContext)
            moveTaskToBack(true)
        }
    }

    companion object {
        const val EXTRA_FROM_BREAK = "from_break"
    }
}
