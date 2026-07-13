package com.ak.momapp.data

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore

// Single process-wide DataStore file shared by all repositories. If the
// file is ever corrupted (bad shutdown mid-write), start over with defaults
// instead of crashing on every launch.
internal val Context.brainBreakDataStore by preferencesDataStore(
    name = "brain_break",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)
