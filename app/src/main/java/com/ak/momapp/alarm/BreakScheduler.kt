package com.ak.momapp.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Thin AlarmManager wrapper. Two independent alarms exist at most:
 * the next break, and a one-shot gentle re-nudge for an ignored break.
 */
object BreakScheduler {

    const val ACTION_BREAK = "com.ak.momapp.action.BREAK"
    const val ACTION_RENUDGE = "com.ak.momapp.action.RENUDGE"
    const val ACTION_SNOOZE = "com.ak.momapp.action.SNOOZE"

    private const val REQUEST_BREAK = 1
    private const val REQUEST_RENUDGE = 2
    private const val REQUEST_SNOOZE = 3

    /** Exact-alarm special access; implicitly granted below Android 12. */
    fun canScheduleExact(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager(context).canScheduleExactAlarms()

    fun scheduleBreak(context: Context, triggerAtMillis: Long) {
        val pi = alarmPendingIntent(context, ACTION_BREAK, REQUEST_BREAK)
        if (canScheduleExact(context)) {
            alarmManager(context).setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, triggerAtMillis, pi,
            )
        } else {
            // No exact-alarm access: fire roughly on time rather than not at
            // all. The settings screen nudges her to grant the access.
            alarmManager(context).setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, triggerAtMillis, pi,
            )
        }
    }

    fun cancelBreak(context: Context) {
        alarmManager(context).cancel(alarmPendingIntent(context, ACTION_BREAK, REQUEST_BREAK))
    }

    /** The re-nudge needn't be exact — a few minutes of drift is fine. */
    fun scheduleRenudge(context: Context, triggerAtMillis: Long) {
        alarmManager(context).setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            alarmPendingIntent(context, ACTION_RENUDGE, REQUEST_RENUDGE),
        )
    }

    fun cancelRenudge(context: Context) {
        alarmManager(context).cancel(alarmPendingIntent(context, ACTION_RENUDGE, REQUEST_RENUDGE))
    }

    /** For the notification's "Snooze 15 min" action button. */
    fun snoozePendingIntent(context: Context): PendingIntent =
        alarmPendingIntent(context, ACTION_SNOOZE, REQUEST_SNOOZE)

    private fun alarmPendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun alarmManager(context: Context): AlarmManager =
        context.getSystemService(AlarmManager::class.java)
}
