package com.catamsp.Daemon.ui.settings.launcher

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.catamsp.Daemon.R
import com.catamsp.Daemon.actions.lock.LockMethod
import com.catamsp.Daemon.actions.openAppsList
import com.catamsp.Daemon.preferences.LauncherPreferences
import com.catamsp.Daemon.preferences.theme.ColorTheme
import com.catamsp.Daemon.setDefaultHomeScreen
import com.catamsp.Daemon.ui.widgets.manage.ManageWidgetPanelsActivity
import com.catamsp.Daemon.ui.widgets.manage.ManageWidgetsActivity


/**
 * The [SettingsFragmentLauncher] is a used as a tab in the SettingsActivity.
 *
 * It is used to change themes, select wallpapers ... theme related stuff
 */
class SettingsFragmentLauncher : PreferenceFragmentCompat() {


    private var sharedPreferencesListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, prefKey ->
            if (prefKey?.startsWith("clock.") == true) {
                updateVisibility()
            }
        }

    private fun updateVisibility() {
        val showSeconds = findPreference<androidx.preference.Preference>(
            LauncherPreferences.clock().keys().showSeconds()
        )
        val timeVisible = LauncherPreferences.clock().timeVisible()
        showSeconds?.isVisible = timeVisible

        val background = findPreference<androidx.preference.Preference>(
            LauncherPreferences.theme().keys().background()
        )
        val lightTheme = LauncherPreferences.theme().colorTheme() == ColorTheme.LIGHT
        background?.isVisible = !lightTheme

        val hidePausedApps = findPreference<androidx.preference.Preference>(
            LauncherPreferences.apps().keys().hidePausedApps()
        )
        hidePausedApps?.isVisible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
    }

    override fun onStart() {
        super.onStart()
        LauncherPreferences.getSharedPreferences()
            .registerOnSharedPreferenceChangeListener(sharedPreferencesListener)
    }

    override fun onPause() {
        LauncherPreferences.getSharedPreferences()
            .unregisterOnSharedPreferenceChangeListener(sharedPreferencesListener)
        super.onPause()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val selectWallpaper = findPreference<androidx.preference.Preference>(
            LauncherPreferences.theme().keys().wallpaper()
        )
        selectWallpaper?.setOnPreferenceClickListener {
            // https://github.com/LineageOS/android_packages_apps_Trebuchet/blob/6caab89b21b2b91f0a439e1fd8c4510dcb255819/src/com/android/launcher3/views/OptionsPopupView.java#L271
            val intent = Intent(Intent.ACTION_SET_WALLPAPER)
                .putExtra("com.android.wallpaper.LAUNCH_SOURCE", "app_launched_launcher")
                .putExtra("com.android.launcher3.WALLPAPER_FLAVOR", "focus_wallpaper")
            startActivity(intent)
            true
        }
        val chooseHomeScreen = findPreference<androidx.preference.Preference>(
            LauncherPreferences.general().keys().chooseHomeScreen()
        )
        chooseHomeScreen?.setOnPreferenceClickListener {
            setDefaultHomeScreen(requireContext(), checkDefault = false)
            true
        }

        val manageWidgets = findPreference<androidx.preference.Preference>(
            LauncherPreferences.widgets().keys().widgets()
        )
        manageWidgets?.setOnPreferenceClickListener {
            startActivity(Intent(requireActivity(), ManageWidgetsActivity::class.java))
            true
        }

        val manageWidgetPanels = findPreference<androidx.preference.Preference>(
            LauncherPreferences.widgets().keys().customPanels()
        )
        manageWidgetPanels?.setOnPreferenceClickListener {
            startActivity(Intent(requireActivity(), ManageWidgetPanelsActivity::class.java))
            true
        }

        val hiddenApps = findPreference<androidx.preference.Preference>(
            LauncherPreferences.apps().keys().hidden()
        )
        hiddenApps?.setOnPreferenceClickListener {
            openAppsList(requireContext(), favorite = false, hidden = true)
            true
        }

        val lockMethod = findPreference<androidx.preference.Preference>(
            LauncherPreferences.actions().keys().lockMethod()
        )

        lockMethod?.setOnPreferenceClickListener {
            LockMethod.chooseMethod(requireContext())
            true
        }

        findPreference<androidx.preference.DropDownPreference>(
            LauncherPreferences.theme().keys().colorTheme()
        )?.apply {
            entries = ColorTheme.entries.filter { x -> x.isAvailable() }
                .map { x -> x.getLabel(requireContext()) }.toTypedArray()
            entryValues = ColorTheme.entries.filter { x -> x.isAvailable() }
                .map { x -> x.name }.toTypedArray()
        }


        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            lockMethod?.isVisible = false
        }

        updateVisibility()
    }
}
