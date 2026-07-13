package com.ak.momapp.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Remembers where today's challenge stands, so closing the app mid-story
 * resumes at the same stage. A state from an older day is simply stale:
 * the ViewModel starts over at stage zero when the day doesn't match.
 */
class ChallengeRepository(private val context: Context) {

    object Keys {
        /** Epoch day the stored stage/done flags belong to. */
        val CHALLENGE_DAY = longPreferencesKey("challenge_day")
        val CHALLENGE_STAGE = intPreferencesKey("challenge_stage")
        val CHALLENGE_DONE = booleanPreferencesKey("challenge_done")
        val CHALLENGES_COMPLETED = intPreferencesKey("challenges_completed")
    }

    data class ChallengeState(
        val day: Long,
        val stage: Int,
        val done: Boolean,
        val totalCompleted: Int,
    )

    val state: Flow<ChallengeState> = context.brainBreakDataStore.data.map { prefs ->
        ChallengeState(
            day = prefs[Keys.CHALLENGE_DAY] ?: -1L,
            stage = prefs[Keys.CHALLENGE_STAGE] ?: 0,
            done = prefs[Keys.CHALLENGE_DONE] ?: false,
            totalCompleted = prefs[Keys.CHALLENGES_COMPLETED] ?: 0,
        )
    }

    suspend fun saveProgress(day: Long, stage: Int) {
        context.brainBreakDataStore.edit {
            it[Keys.CHALLENGE_DAY] = day
            it[Keys.CHALLENGE_STAGE] = stage
            it[Keys.CHALLENGE_DONE] = false
        }
    }

    suspend fun markCompleted(day: Long) {
        context.brainBreakDataStore.edit {
            it[Keys.CHALLENGE_DAY] = day
            it[Keys.CHALLENGE_DONE] = true
            it[Keys.CHALLENGES_COMPLETED] = (it[Keys.CHALLENGES_COMPLETED] ?: 0) + 1
        }
    }
}
