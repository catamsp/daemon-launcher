package com.catamsp.Daemon.apps

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.LruCache
import com.catamsp.Daemon.preferences.LauncherPreferences

object CustomIconManager {

    private const val PREF_KEY = "settings_icons_custom_icons_key"

    private var overrides: HashMap<String, String> = HashMap()
    private val iconCache = LruCache<String, Drawable>(50)
    private var initialized = false
    private var appContext: Context? = null

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        appContext = context.applicationContext
        loadOverrides()
    }

    private fun loadOverrides() {
        val prefs = LauncherPreferences.getSharedPreferences()
        val rawSet = prefs.getStringSet(PREF_KEY, null)
        if (rawSet != null) {
            overrides = HashMap()
            for (entry in rawSet) {
                val idx = entry.indexOf('=')
                if (idx > 0) {
                    overrides[entry.substring(0, idx)] = entry.substring(idx + 1)
                }
            }
        }
    }

    private fun saveOverrides() {
        val entries = overrides.map { "${it.key}=${it.value}" }.toSet()
        LauncherPreferences.getSharedPreferences().edit()
            .putStringSet(PREF_KEY, entries)
            .apply()
    }

    fun getIcon(app: AbstractAppInfo): Drawable? {
        val packageName = when (app) {
            is AppInfo -> app.packageName
            is PinnedShortcutInfo -> app.packageName
            else -> return null
        }

        val uriString = overrides[packageName] ?: return null

        iconCache.get(uriString)?.let { return it }

        return try {
            val drawable = if (uriString.startsWith("iconpack:")) {
                val drawableName = uriString.removePrefix("iconpack:")
                val ctx = appContext ?: return null
                IconPackManager.getInstance(ctx).getDrawableByName(drawableName)
            } else {
                val uri = Uri.parse(uriString)
                val ctx = appContext ?: return null
                ctx.contentResolver.openInputStream(uri)?.use { stream ->
                    Drawable.createFromStream(stream, uri.toString())
                }
            }
            if (drawable != null) {
                iconCache.put(uriString, drawable)
            }
            drawable
        } catch (e: Exception) {
            null
        }
    }

    fun setIcon(app: AbstractAppInfo, uri: Uri) {
        val packageName = when (app) {
            is AppInfo -> app.packageName
            is PinnedShortcutInfo -> app.packageName
            else -> return
        }
        overrides[packageName] = uri.toString()
        saveOverrides()
        iconCache.evictAll()
    }

    fun setIconFromIconPack(app: AbstractAppInfo, drawableName: String) {
        val packageName = when (app) {
            is AppInfo -> app.packageName
            is PinnedShortcutInfo -> app.packageName
            else -> return
        }
        overrides[packageName] = "iconpack:$drawableName"
        saveOverrides()
        iconCache.evictAll()
    }

    fun removeIcon(app: AbstractAppInfo) {
        val packageName = when (app) {
            is AppInfo -> app.packageName
            is PinnedShortcutInfo -> app.packageName
            else -> return
        }
        overrides.remove(packageName)
        saveOverrides()
        iconCache.evictAll()
    }

    fun hasCustomIcon(app: AbstractAppInfo): Boolean {
        val packageName = when (app) {
            is AppInfo -> app.packageName
            is PinnedShortcutInfo -> app.packageName
            else -> return false
        }
        return overrides.containsKey(packageName)
    }
}
