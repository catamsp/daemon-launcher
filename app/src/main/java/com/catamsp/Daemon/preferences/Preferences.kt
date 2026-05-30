package com.catamsp.Daemon.preferences

import android.content.Context
import android.util.Log
import com.catamsp.Daemon.BuildConfig
import com.catamsp.Daemon.actions.Action
import com.catamsp.Daemon.apps.AbstractAppInfo
import com.catamsp.Daemon.apps.AbstractAppInfo.Companion.INVALID_USER
import com.catamsp.Daemon.apps.AppInfo
import com.catamsp.Daemon.apps.DetailedAppInfo
import com.catamsp.Daemon.preferences.legacy.migratePreferencesFromVersion1
import com.catamsp.Daemon.preferences.legacy.migratePreferencesFromVersion100
import com.catamsp.Daemon.preferences.legacy.migratePreferencesFromVersion2
import com.catamsp.Daemon.preferences.legacy.migratePreferencesFromVersion3
import com.catamsp.Daemon.preferences.legacy.migratePreferencesFromVersion4
import com.catamsp.Daemon.preferences.legacy.migratePreferencesFromVersionUnknown
import com.catamsp.Daemon.sendCrashNotification
import com.catamsp.Daemon.ui.HomeActivity
import com.catamsp.Daemon.widgets.ClockWidget
import com.catamsp.Daemon.widgets.DebugInfoWidget
import com.catamsp.Daemon.widgets.WidgetPanel
import com.catamsp.Daemon.widgets.WidgetPosition
import com.catamsp.Daemon.widgets.generateInternalId
import com.catamsp.Daemon.widgets.getAppWidgetHost

/* Current version of the structure of preferences.
 * Increase when breaking changes are introduced and write an appropriate case in
 * `migratePreferencesToNewVersion`
 */
const val PREFERENCE_VERSION = 103
const val UNKNOWN_PREFERENCE_VERSION = -1
private const val TAG = "Launcher - Preferences"


/*
 * Tries to detect preferences written by older versions of the app
 * and migrate them to the current format.
 */
fun migratePreferencesToNewVersion(context: Context) {
    try {
        when (LauncherPreferences.internal().versionCode()) {
            // Check versions, make sure transitions between versions go well
            PREFERENCE_VERSION -> { /* the version installed and used previously are the same */
            }

            UNKNOWN_PREFERENCE_VERSION -> { /* still using the old preferences file */
                migratePreferencesFromVersionUnknown(context)
                Log.i(
                    TAG,
                    "migration of preferences  complete (${UNKNOWN_PREFERENCE_VERSION} -> ${PREFERENCE_VERSION})."
                )
            }

            1 -> {
                migratePreferencesFromVersion1(context)
                Log.i(TAG, "migration of preferences  complete (1 -> ${PREFERENCE_VERSION}).")
            }

            2 -> {
                migratePreferencesFromVersion2(context)
                Log.i(TAG, "migration of preferences  complete (2 -> ${PREFERENCE_VERSION}).")
            }

            3 -> {
                migratePreferencesFromVersion3(context)
                Log.i(TAG, "migration of preferences  complete (3 -> ${PREFERENCE_VERSION}).")
            }

            // There was a bug where instead of the preference version the app version was written.
            in 4..99 -> {
                migratePreferencesFromVersion4(context)
                Log.i(TAG, "migration of preferences  complete (4 -> ${PREFERENCE_VERSION}).")
            }

            100 -> {
                migratePreferencesFromVersion100(context)
                Log.i(TAG, "migration of preferences  complete (100 -> ${PREFERENCE_VERSION}).")
            }

            101 -> {
                // Fix for renamed/deleted animation enums causing crashes (e.g. LIQUID)
                val prefs = LauncherPreferences.getSharedPreferences()
                prefs.edit().apply {
                    remove(LauncherPreferences.animations().keys().swipeUp())
                    remove(LauncherPreferences.animations().keys().swipeDown())
                    remove(LauncherPreferences.animations().keys().swipeLeft())
                    remove(LauncherPreferences.animations().keys().swipeRight())
                    remove(LauncherPreferences.animations().keys().other())
                    apply()
                }
                LauncherPreferences.internal().versionCode(102)
                migratePreferencesToNewVersion(context)
            }

            102 -> {
                // Migration to 48x48 grid
                val widgets = LauncherPreferences.widgets().widgets()?.map { widget ->
                    widget.position.x = (widget.position.x * 4).toShort()
                    widget.position.y = (widget.position.y * 4).toShort()
                    widget.position.width = (widget.position.width * 4).toShort()
                    widget.position.height = (widget.position.height * 4).toShort()
                    widget
                }?.toSet()
                LauncherPreferences.widgets().widgets(widgets)
                LauncherPreferences.internal().versionCode(PREFERENCE_VERSION)
                Log.i(TAG, "migration of preferences complete (102 -> $PREFERENCE_VERSION).")
            }

            else -> {
                Log.w(
                    TAG,
                    "Shared preferences were written by a newer version of the app (${
                        LauncherPreferences.internal().versionCode()
                    })!"
                )
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Unable to restore preferences", e)
        sendCrashNotification(context, e)
        resetPreferences(context)
    }
}

fun resetPreferences(context: Context) {
    Log.i(TAG, "Resetting preferences")
    LauncherPreferences.clear()
    LauncherPreferences.internal().versionCode(PREFERENCE_VERSION)
    context.getAppWidgetHost().deleteHost()

    LauncherPreferences.widgets().widgets(
        setOf(
            ClockWidget(
                generateInternalId(),
                WidgetPosition(4, 12, 40, 16),
                WidgetPanel.HOME.id
            )
        )
    )

    if (BuildConfig.DEBUG) {
        LauncherPreferences.widgets().widgets(
            LauncherPreferences.widgets().widgets().also {
                it.add(
                    DebugInfoWidget(
                        generateInternalId(),
                        WidgetPosition(4, 4, 40, 16),
                        WidgetPanel.HOME.id
                    )
                )
            }
        )
    }

    val hidden: MutableSet<AbstractAppInfo> = mutableSetOf()

    if (!BuildConfig.DEBUG) {
        val launcher = DetailedAppInfo.fromAppInfo(
            AppInfo(
                BuildConfig.APPLICATION_ID,
                HomeActivity::class.java.name,
                INVALID_USER
            ), context
        )
        launcher?.getRawInfo()?.let { hidden.add(it) }
        Log.i(TAG, "Hiding ${launcher?.getRawInfo()}")
    }
    LauncherPreferences.apps().hidden(hidden)

    Action.resetToDefaultActions(context)
}
