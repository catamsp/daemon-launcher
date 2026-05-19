package com.catamsp.Daemon.preferences.legacy

import android.content.Context
import com.catamsp.Daemon.preferences.LauncherPreferences
import com.catamsp.Daemon.widgets.ClockWidget
import com.catamsp.Daemon.widgets.WidgetPanel
import com.catamsp.Daemon.widgets.WidgetPosition
import com.catamsp.Daemon.widgets.generateInternalId

fun migratePreferencesFromVersion4(context: Context) {
    assert(LauncherPreferences.internal().versionCode() < 100)

    LauncherPreferences.widgets().widgets(
        setOf(
            ClockWidget(
                generateInternalId(),
                WidgetPosition(1, 3, 10, 4),
                WidgetPanel.HOME.id
            )
        )
    )
    LauncherPreferences.internal().versionCode(100)
    migratePreferencesFromVersion100(context)
}