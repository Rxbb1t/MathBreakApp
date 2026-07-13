package com.ak.momapp.alarm

import com.ak.momapp.data.BrainBreakSettings
import java.time.LocalDateTime

/**
 * Pure "when is the next break?" logic, kept free of Android classes so it
 * can be unit-tested. The rule: the next break is one interval from now; if
 * that lands outside the active window (too early, too late, or on an off
 * day), it slides to the start of the next active window instead.
 */
object NextBreakCalculator {

    /** Returns the next break time after [now], or null if no days are active. */
    fun next(now: LocalDateTime, settings: BrainBreakSettings): LocalDateTime? {
        if (settings.activeDays.isEmpty()) return null
        val candidate = now.plusHours(settings.intervalHours.toLong())
        if (isInActiveWindow(candidate, settings)) return candidate
        return nextWindowStart(candidate, settings)
    }

    fun isInActiveWindow(time: LocalDateTime, settings: BrainBreakSettings): Boolean {
        if (time.dayOfWeek !in settings.activeDays) return false
        val minuteOfDay = time.hour * 60 + time.minute
        return minuteOfDay in settings.activeStartMinutes..settings.activeEndMinutes
    }

    private fun nextWindowStart(after: LocalDateTime, settings: BrainBreakSettings): LocalDateTime {
        // At most 7 days until an active day comes around again.
        for (daysAhead in 0..7L) {
            val date = after.toLocalDate().plusDays(daysAhead)
            if (date.dayOfWeek !in settings.activeDays) continue
            val windowStart = date.atStartOfDay()
                .plusMinutes(settings.activeStartMinutes.toLong())
            if (windowStart > after) return windowStart
        }
        error("unreachable: activeDays is non-empty")
    }
}
