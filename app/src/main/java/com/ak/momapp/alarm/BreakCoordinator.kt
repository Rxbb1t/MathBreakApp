package com.ak.momapp.alarm

import android.content.Context
import com.ak.momapp.data.BreakStateRepository
import com.ak.momapp.data.SettingsRepository
import com.ak.momapp.notify.BreakNotifier
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.flow.first

/**
 * The one place that decides what happens around a break: scheduling the
 * next one, reacting to the alarm, snoozing, and clearing up after an
 * answer. Everything is idempotent so callers can be generous.
 */
object BreakCoordinator {

    private const val SNOOZE_MINUTES = 15L
    private const val RENUDGE_MINUTES = 20L

    /**
     * Re-arms the alarm without changing the plan: keeps a stored future
     * break time if there is one, otherwise computes a fresh one. Safe to
     * call on every app open and after every reboot.
     */
    suspend fun ensureScheduled(context: Context) {
        val settings = SettingsRepository(context).settings.first()
        if (!settings.remindersEnabled || settings.activeDays.isEmpty()) {
            cancelAll(context)
            return
        }
        val stored = BreakStateRepository(context).nextBreakAt.first()
        if (stored != null && stored > System.currentTimeMillis()) {
            BreakScheduler.scheduleBreak(context, stored)
        } else {
            rescheduleFromNow(context)
        }
    }

    /** Starts a fresh cadence from now. Call after any settings change. */
    suspend fun rescheduleFromNow(context: Context) {
        val settings = SettingsRepository(context).settings.first()
        val next = if (settings.remindersEnabled) {
            NextBreakCalculator.next(LocalDateTime.now(), settings)
        } else {
            null
        }
        if (next == null) {
            cancelAll(context)
        } else {
            scheduleAt(context, next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
        }
    }

    /** The break alarm fired: ping her, plan the re-nudge and the next break. */
    suspend fun onBreakFired(context: Context) {
        val settings = SettingsRepository(context).settings.first()
        if (!settings.remindersEnabled) {
            cancelAll(context)
            return
        }
        BreakNotifier.showBreak(context, settings.language)
        BreakScheduler.scheduleRenudge(context, millisFromNow(RENUDGE_MINUTES))
        rescheduleFromNow(context)
    }

    /** One quiet reminder, and only if the break is still unanswered. */
    suspend fun onRenudgeFired(context: Context) {
        if (BreakNotifier.isBreakShowing(context)) {
            val language = SettingsRepository(context).settings.first().language
            BreakNotifier.showRenudge(context, language)
        }
    }

    /** From the notification action or the in-app button. */
    suspend fun snooze(context: Context) {
        BreakNotifier.cancel(context)
        BreakScheduler.cancelRenudge(context)
        scheduleAt(context, millisFromNow(SNOOZE_MINUTES))
    }

    /** She finished a problem — whatever was pending is settled. */
    suspend fun breakCompleted(context: Context) {
        BreakNotifier.cancel(context)
        BreakScheduler.cancelRenudge(context)
    }

    private suspend fun scheduleAt(context: Context, epochMillis: Long) {
        BreakScheduler.scheduleBreak(context, epochMillis)
        BreakStateRepository(context).setNextBreakAt(epochMillis)
    }

    private suspend fun cancelAll(context: Context) {
        BreakScheduler.cancelBreak(context)
        BreakScheduler.cancelRenudge(context)
        BreakStateRepository(context).setNextBreakAt(null)
    }

    private fun millisFromNow(minutes: Long): Long =
        System.currentTimeMillis() + minutes * 60_000
}
