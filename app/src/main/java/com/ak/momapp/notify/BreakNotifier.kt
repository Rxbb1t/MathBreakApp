package com.ak.momapp.notify

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ak.momapp.MainActivity
import com.ak.momapp.R
import com.ak.momapp.alarm.BreakScheduler
import com.ak.momapp.i18n.AppLanguage
import com.ak.momapp.i18n.strings
import com.ak.momapp.problem.PersonalContent

object BreakNotifier {

    const val CHANNEL_BREAKS = "breaks"
    const val CHANNEL_GENTLE = "gentle_reminders"
    const val NOTIFICATION_ID = 100

    private const val REQUEST_OPEN_PROBLEM = 10

    fun createChannels(context: Context) {
        val manager = NotificationManagerCompat.from(context)
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_BREAKS, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName("Brain breaks")
                .setDescription("The scheduled math-break pings")
                .build(),
        )
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_GENTLE, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName("Gentle reminders")
                .setDescription("A quiet nudge when a break was missed")
                .build(),
        )
    }

    fun showBreak(context: Context, language: AppLanguage) {
        val line = PersonalContent.notificationLines(language).randomOrNull()
            ?: "Time for a quick brain break"
        notify(context, buildNotification(context, CHANNEL_BREAKS, line, language))
    }

    /** Replaces the break notification with a quieter, softer one. */
    fun showRenudge(context: Context, language: AppLanguage) {
        notify(
            context,
            buildNotification(context, CHANNEL_GENTLE, PersonalContent.renudgeLine(language), language),
        )
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    /** True while a posted break (or re-nudge) hasn't been answered or snoozed. */
    fun isBreakShowing(context: Context): Boolean =
        context.getSystemService(NotificationManager::class.java)
            .activeNotifications.any { it.id == NOTIFICATION_ID }

    private fun buildNotification(
        context: Context,
        channel: String,
        text: String,
        language: AppLanguage,
    ): android.app.Notification {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_FROM_BREAK, true)
        }
        val contentPi = PendingIntent.getActivity(
            context,
            REQUEST_OPEN_PROBLEM,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Brain Break")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(contentPi)
            .addAction(0, language.strings().snooze15, BreakScheduler.snoozePendingIntent(context))
            // Sticks around until answered or snoozed (on Android 14+ she
            // can still swipe it away, which is fine).
            .setOngoing(true)
            .build()
    }

    private fun notify(context: Context, notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
