package com.catamsp.Daemon

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.UserHandle
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.catamsp.Daemon.actions.TorchManager
import com.catamsp.Daemon.apps.AbstractAppInfo
import com.catamsp.Daemon.apps.AbstractDetailedAppInfo
import com.catamsp.Daemon.apps.AppIconFetcher
import com.catamsp.Daemon.apps.AppInfo
import com.catamsp.Daemon.apps.isPrivateSpaceLocked
import com.catamsp.Daemon.preferences.LauncherPreferences
import com.catamsp.Daemon.preferences.migratePreferencesToNewVersion
import com.catamsp.Daemon.preferences.resetPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.system.exitProcess


const val APP_WIDGET_HOST_ID = 42


class Application : android.app.Application(), ImageLoaderFactory {
    val apps = MutableLiveData<List<AbstractDetailedAppInfo>>()
    val privateSpaceLocked = MutableLiveData<Boolean>()
    lateinit var appWidgetHost: AppWidgetHost
    lateinit var appWidgetManager: AppWidgetManager

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(AppIconFetcher.Factory(this@Application))
            }
            .build()
    }

    private val profileAvailabilityBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // TODO: only update specific apps
            // use Intent.EXTRA_USER
            loadApps()
        }
    }

    // TODO: only update specific apps
    private val launcherAppsCallback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) {
            val currentApps = apps.value?.toMutableList() ?: return
            currentApps.removeAll { it.getRawInfo().let { info -> info is AppInfo && info.packageName == packageName } && it.getUser(this@Application) == user }
            apps.postValue(currentApps)
        }

        override fun onPackageAdded(packageName: String?, user: UserHandle?) {
            val currentApps = apps.value?.toMutableList() ?: mutableListOf()
            val launcherApps = getSystemService(LAUNCHER_APPS_SERVICE) as LauncherApps
            val privateSpaceUser = com.catamsp.Daemon.apps.getPrivateSpaceUser(this@Application)
            launcherApps.getActivityList(packageName, user).forEach {
                currentApps.add(com.catamsp.Daemon.apps.DetailedAppInfo(it, it.user == privateSpaceUser, this@Application))
            }
            currentApps.sortBy { it.getCustomLabel(this@Application) }
            apps.postValue(currentApps)
        }

        override fun onPackageChanged(packageName: String?, user: UserHandle?) {
            val currentApps = apps.value?.toMutableList() ?: return
            val privateSpaceUser = com.catamsp.Daemon.apps.getPrivateSpaceUser(this@Application)
            currentApps.removeAll { it.getRawInfo().let { info -> info is AppInfo && info.packageName == packageName } && it.getUser(this@Application) == user }
            val launcherApps = getSystemService(LAUNCHER_APPS_SERVICE) as LauncherApps
            launcherApps.getActivityList(packageName, user).forEach {
                currentApps.add(com.catamsp.Daemon.apps.DetailedAppInfo(it, it.user == privateSpaceUser, this@Application))
            }
            currentApps.sortBy { it.getCustomLabel(this@Application) }
            apps.postValue(currentApps)
        }

        override fun onPackagesAvailable(p0: Array<out String>?, p1: UserHandle?, p2: Boolean) {
            loadApps()
        }

        override fun onPackagesSuspended(packageNames: Array<out String>?, user: UserHandle?) {
            loadApps()
        }

        override fun onPackagesUnsuspended(packageNames: Array<out String>?, user: UserHandle?) {
            loadApps()
        }

        override fun onPackagesUnavailable(p0: Array<out String>?, p1: UserHandle?, p2: Boolean) {
            loadApps()
        }

        override fun onPackageLoadingProgressChanged(
            packageName: String,
            user: UserHandle,
            progress: Float
        ) {
            // TODO
        }

        override fun onShortcutsChanged(
            packageName: String,
            shortcuts: MutableList<ShortcutInfo>,
            user: UserHandle
        ) {
            loadApps()
        }
    }

    var torchManager: TorchManager? = null
    private var customAppNames: HashMap<AbstractAppInfo, String>? = null
    private var distractingApps: Set<AbstractAppInfo>? = null
    private var widgets: Set<com.catamsp.Daemon.widgets.Widget>? = null

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, pref ->
        if (pref == getString(R.string.settings_apps_custom_names_key)) {
            customAppNames = LauncherPreferences.apps().customNames()
        } else if (pref == getString(R.string.settings_apps_distracting_apps_key)) {
            distractingApps = LauncherPreferences.apps().distractingApps()
        } else if (pref == getString(R.string.settings_widgets_widgets_key)) {
            widgets = LauncherPreferences.widgets().widgets()
        } else if (pref == LauncherPreferences.apps().keys().pinnedShortcuts()) {
            loadApps()
        }
    }

    override fun onCreate() {
        super.onCreate()
        // TODO  Error: Invalid resource ID 0x00000000.
        // DynamicColors.applyToActivitiesIfAvailable(this)

        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            sendCrashNotification(this@Application, throwable)
            exitProcess(1)
        }


        if (Build.VERSION.SDK_INT >= VERSION_CODES.M) {
            torchManager = TorchManager(this)
        }

        appWidgetHost = AppWidgetHost(this.applicationContext, APP_WIDGET_HOST_ID)
        appWidgetManager = AppWidgetManager.getInstance(this.applicationContext)


        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        LauncherPreferences.init(preferences, this.resources)


        // Try to restore old preferences
        migratePreferencesToNewVersion(this)

        // First time opening the app: set defaults
        // The tutorial is started from HomeActivity#onStart, as starting it here is blocked by android
        if (!LauncherPreferences.internal().started()) {
            resetPreferences(this)
        }


        LauncherPreferences.getSharedPreferences()
            .registerOnSharedPreferenceChangeListener(listener)


        val launcherApps = getSystemService(LAUNCHER_APPS_SERVICE) as LauncherApps
        launcherApps.registerCallback(launcherAppsCallback)

        if (Build.VERSION.SDK_INT >= VERSION_CODES.N) {
            val filter = IntentFilter().also {
                if (Build.VERSION.SDK_INT >= VERSION_CODES.VANILLA_ICE_CREAM) {
                    it.addAction(Intent.ACTION_PROFILE_AVAILABLE)
                    it.addAction(Intent.ACTION_PROFILE_UNAVAILABLE)
                } else {
                    it.addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE)
                    it.addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
                }
            }
            ContextCompat.registerReceiver(
                this, profileAvailabilityBroadcastReceiver, filter,
                ContextCompat.RECEIVER_EXPORTED
            )
        }

        if (Build.VERSION.SDK_INT >= VERSION_CODES.N_MR1) {
            removeUnusedShortcuts(this)
        }
        loadApps()

        createNotificationChannels(this)
    }

    fun getCustomAppNames(): HashMap<AbstractAppInfo, String> {
        return (customAppNames ?: LauncherPreferences.apps().customNames() ?: HashMap())
            .also { customAppNames = it }
    }

    fun getDistractingApps(): Set<AbstractAppInfo> {
        return (distractingApps ?: LauncherPreferences.apps().distractingApps() ?: HashSet())
            .also { distractingApps = it }
    }

    fun getWidgets(): Set<com.catamsp.Daemon.widgets.Widget> {
        return (widgets ?: LauncherPreferences.widgets().widgets() ?: HashSet())
            .also { widgets = it }
    }

    private fun loadApps() {
        privateSpaceLocked.postValue(isPrivateSpaceLocked(this))
        CoroutineScope(Dispatchers.Default).launch {
            apps.postValue(getApps(packageManager, applicationContext))
        }
    }
}
