package com.ak.momapp.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Explicit-intent target for the app's own alarms. */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                when (intent.action) {
                    BreakScheduler.ACTION_BREAK -> BreakCoordinator.onBreakFired(app)
                    BreakScheduler.ACTION_RENUDGE -> BreakCoordinator.onRenudgeFired(app)
                    BreakScheduler.ACTION_SNOOZE -> BreakCoordinator.snooze(app)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
