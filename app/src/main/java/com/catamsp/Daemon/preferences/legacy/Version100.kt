package com.catamsp.Daemon.preferences.legacy

import android.content.Context
import com.catamsp.Daemon.Application
import com.catamsp.Daemon.preferences.LauncherPreferences
import com.catamsp.Daemon.preferences.PREFERENCE_VERSION
import com.catamsp.Daemon.widgets.ClockWidget
import com.catamsp.Daemon.widgets.DebugInfoWidget
import com.catamsp.Daemon.widgets.generateInternalId
import com.catamsp.Daemon.widgets.updateWidget

fun migratePreferencesFromVersion100(context: Context) {
    assert(PREFERENCE_VERSION == 101)
    assert(LauncherPreferences.internal().versionCode() == 100)

    val widgets = LauncherPreferences.widgets().widgets() ?: setOf()
    widgets.forEach { widget ->
        when (widget) {
            is ClockWidget -> {
                val id = widget.id
                val newId = generateInternalId()
                (context.applicationContext as Application).appWidgetHost.deleteAppWidgetId(id)
                widget.delete(context)
                widget.id = newId
                updateWidget(widget)
            }

            is DebugInfoWidget -> {
                val id = widget.id
                val newId = generateInternalId()
                (context.applicationContext as Application).appWidgetHost.deleteAppWidgetId(id)
                widget.delete(context)
                widget.id = newId
                updateWidget(widget)
            }

            else -> {}
        }
    }
    LauncherPreferences.internal().versionCode(101)
}