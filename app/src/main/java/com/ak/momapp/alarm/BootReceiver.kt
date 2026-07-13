package com.ak.momapp.alarm

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Alarms don't survive a reboot (or an app update), so re-arm from the
 * persisted schedule. Also re-arms when the user grants exact-alarm access
 * so the pending alarm is upgraded from inexact to exact.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
            -> {
                val app = context.applicationContext
                val pending = goAsync()
                CoroutineScope(Dispatchers.Default).launch {
                    try {
                        BreakCoordinator.ensureScheduled(app)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }
}
