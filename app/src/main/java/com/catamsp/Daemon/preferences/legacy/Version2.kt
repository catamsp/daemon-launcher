package com.catamsp.Daemon.preferences.legacy

import android.content.Context
import com.catamsp.Daemon.actions.Action
import com.catamsp.Daemon.actions.Gesture
import com.catamsp.Daemon.actions.LauncherAction
import com.catamsp.Daemon.preferences.LauncherPreferences
import com.catamsp.Daemon.preferences.PREFERENCE_VERSION


/**
 * Migrate preferences from version 2 (used until version 0.0.21) to the current format
 * (see [PREFERENCE_VERSION])
 */
fun migratePreferencesFromVersion2(context: Context) {
    assert(LauncherPreferences.internal().versionCode() == 2)
    // previously there was no setting for this
    Action.setActionForGesture(Gesture.BACK, LauncherAction.CHOOSE)
    LauncherPreferences.internal().versionCode(3)
    migratePreferencesFromVersion3(context)
}