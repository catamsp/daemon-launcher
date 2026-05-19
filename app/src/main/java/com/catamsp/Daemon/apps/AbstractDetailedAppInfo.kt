package com.catamsp.Daemon.apps

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.UserHandle
import android.util.Log
import com.catamsp.Daemon.Application
import com.catamsp.Daemon.actions.Action
import com.catamsp.Daemon.preferences.LauncherPreferences

/**
 * This interface is implemented by [DetailedAppInfo] and [DetailedPinnedShortcutInfo]
 */
sealed interface AbstractDetailedAppInfo {
    fun getRawInfo(): AbstractAppInfo
    fun getLabel(): String
    fun getIcon(context: Context): Drawable
    fun getUser(context: Context): UserHandle
    fun isPrivate(): Boolean
    fun isRemovable(): Boolean
    fun getAction(): Action


    fun getNormalizedLabel(context: Context): String {
        return unicodeNormalize(getCustomLabel(context))
    }

    private fun unicodeNormalize(s: String): String {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            val normalizer = android.icu.text.Normalizer2.getNFKDInstance()
            return normalizer.normalize(s.lowercase(java.util.Locale.ROOT))
        }
        return s.lowercase(java.util.Locale.ROOT)
    }


    fun getCustomLabel(context: Context): String {
        val map = (context.applicationContext as? Application)?.getCustomAppNames()
        return map?.get(getRawInfo()) ?: getLabel()
    }


    fun setCustomLabel(label: CharSequence?) {
        Log.i("Launcher", "Setting custom label for ${this.getRawInfo()} to ${label}.")
        val map = LauncherPreferences.apps().customNames() ?: HashMap<AbstractAppInfo, String>()

        if (label.isNullOrEmpty()) {
            map.remove(getRawInfo())
        } else {
            map[getRawInfo()] = label.toString()
        }
        LauncherPreferences.apps().customNames(map)
    }

}