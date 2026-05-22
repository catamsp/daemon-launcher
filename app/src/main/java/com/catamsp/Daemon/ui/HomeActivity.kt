package com.catamsp.Daemon.ui

import android.content.SharedPreferences
import android.content.res.Resources
import android.os.Bundle
import android.view.View
import com.catamsp.Daemon.Application
import com.catamsp.Daemon.actions.Action
import com.catamsp.Daemon.actions.Gesture
import com.catamsp.Daemon.actions.LauncherAction
import com.catamsp.Daemon.databinding.ActivityHomeBinding
import com.catamsp.Daemon.openTutorial
import com.catamsp.Daemon.preferences.LauncherPreferences
import com.catamsp.Daemon.ui.util.LauncherGestureActivity
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat


/**
 * [HomeActivity] is the actual application launcher.
 * It displays widgets (usually just the clock)
 * and listens for gestures.
 */
class HomeActivity : UIObject, LauncherGestureActivity() {

    private lateinit var binding: ActivityHomeBinding

    private fun checkNotificationPermission() {
        // Permission check is now silent as requested
    }

    private var sharedPreferencesListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, prefKey ->
            if (prefKey?.startsWith("clock.") == true) {
                binding.homeWidgetContainer.updateWidgets(
                    this@HomeActivity,
                    LauncherPreferences.widgets().widgets()
                )
            } else if (prefKey?.startsWith("display.") == true) {
                setWindowFlags(window, isHomeScreen())
                if (prefKey == "display.rotate_screen") {
                    requestedOrientation = if (LauncherPreferences.display().rotateScreen()) {
                        android.content.pm.ActivityInfo.SCREEN_ORIENTATION_USER
                    } else {
                        android.content.pm.ActivityInfo.SCREEN_ORIENTATION_NOSENSOR
                    }
                }
            } else if (prefKey?.startsWith("action.") == true) {
                updateSettingsFallbackButtonVisibility()
            } else if (prefKey == LauncherPreferences.widgets().keys().widgets()) {
                binding.homeWidgetContainer.updateWidgets(
                    this@HomeActivity,
                    LauncherPreferences.widgets().widgets()
                )
            }

        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super<LauncherGestureActivity>.onCreate(savedInstanceState)
        super<UIObject>.onCreate()

        // Initialise layout
        binding = ActivityHomeBinding.inflate(layoutInflater)

        setContentView(binding.root)

        binding.buttonFallbackSettings.setOnClickListener {
            LauncherAction.SETTINGS.invoke(this)
        }
    }

    override fun onStart() {
        super<LauncherGestureActivity>.onStart()
        super<UIObject>.onStart()

        // If the tutorial was not finished, start it
        if (!LauncherPreferences.internal().started()) {
            openTutorial(this)
        }

        LauncherPreferences.getSharedPreferences()
            .registerOnSharedPreferenceChangeListener(sharedPreferencesListener)

    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus && LauncherPreferences.display().hideNavigationBar()) {
            hideNavigationBar()
        }
    }

    private fun updateSettingsFallbackButtonVisibility() {
        // If Daemon launcher settings can not be reached from any action bound to an enabled gesture,
        // show the fallback button.
        binding.buttonFallbackSettings.visibility = if (
            !Gesture.entries.any { g ->
                g.isEnabled() && Action.forGesture(g)?.canReachSettings() == true
            }
        ) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    override fun getTheme(): Resources.Theme {
        return modifyTheme(super.getTheme())
    }

    override fun onPause() {
        try {
            (application as Application).appWidgetHost.stopListening()
        } catch (e: Exception) {
            // Throws a NullPointerException on Android 12 an earlier, see #172
            e.printStackTrace()
        }
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        updateSettingsFallbackButtonVisibility()
        checkNotificationPermission()

        binding.homeWidgetContainer.updateWidgets(
            this@HomeActivity,
            LauncherPreferences.widgets().widgets()
        )

        (application as Application).appWidgetHost.startListening()
    }


    override fun onDestroy() {
        LauncherPreferences.getSharedPreferences()
            .unregisterOnSharedPreferenceChangeListener(sharedPreferencesListener)
        super.onDestroy()
    }

    override fun handleBack() {
        Gesture.BACK(this)
    }

    override fun getRootView(): View {
        return binding.root
    }

    override fun isHomeScreen(): Boolean {
        return true
    }
}
