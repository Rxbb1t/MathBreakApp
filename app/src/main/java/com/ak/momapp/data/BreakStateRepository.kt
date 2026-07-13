package com.ak.momapp.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The one piece of alarm state worth persisting: when the next break is due
 * (epoch millis). It survives reboots so BootReceiver can re-arm the alarm
 * at the original time, and the settings screen shows it to the user.
 */
class BreakStateRepository(private val context: Context) {

    object Keys {
        val NEXT_BREAK_AT = longPreferencesKey("next_break_at")
    }

    val nextBreakAt: Flow<Long?> =
        context.brainBreakDataStore.data.map { it[Keys.NEXT_BREAK_AT] }

    suspend fun setNextBreakAt(epochMillis: Long?) {
        context.brainBreakDataStore.edit { prefs ->
            if (epochMillis == null) prefs.remove(Keys.NEXT_BREAK_AT)
            else prefs[Keys.NEXT_BREAK_AT] = epochMillis
        }
    }
}
