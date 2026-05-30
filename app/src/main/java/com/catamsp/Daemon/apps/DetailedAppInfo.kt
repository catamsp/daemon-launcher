package com.catamsp.Daemon.apps

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.graphics.drawable.Drawable
import android.os.UserHandle
import androidx.appcompat.content.res.AppCompatResources
import com.catamsp.Daemon.R
import com.catamsp.Daemon.actions.Action
import com.catamsp.Daemon.actions.AppAction
import com.catamsp.Daemon.getUserId
import com.catamsp.Daemon.getUserFromId

/**
 * Stores information used to create [com.catamsp.Daemon.ui.list.apps.AppsRecyclerAdapter] rows.
 */
class DetailedAppInfo(
    private val app: AppInfo,
    private val label: CharSequence,
    private val privateSpace: Boolean,
    private val removable: Boolean = true,
) : AbstractDetailedAppInfo {

    constructor(activityInfo: LauncherActivityInfo, private: Boolean, context: Context) : this(
        AppInfo(
            activityInfo.applicationInfo.packageName,
            activityInfo.name,
            getUserId(activityInfo.user, context)
        ),
        activityInfo.label,
        private,
        // App can be uninstalled iff it is not a system app
        activityInfo.applicationInfo.flags.and(ApplicationInfo.FLAG_SYSTEM) == 0
    )


    override fun getLabel(): String {
        return label.toString()
    }

    override fun getIcon(context: Context): Drawable {
        // 1. Per-app custom override
        val customIcon = CustomIconManager.getIcon(app)
        if (customIcon != null) return applyThemeIfNeeded(context, customIcon)

        // 2. Icon pack (exact match OR fallback compositing)
        val componentName = app.getComponentName()
        if (componentName != null) {
            val systemIcon = app.getLauncherActivityInfo(context)?.getBadgedIcon(0)
            if (systemIcon != null) {
                val packIcon = IconPackManager.getInstance(context).getIcon(componentName, systemIcon)
                if (packIcon != null) return applyThemeIfNeeded(context, packIcon)
            }
        }

        // 3. System icon (no icon pack, or no match and no fallback)
        val systemIcon = app.getLauncherActivityInfo(context)?.getBadgedIcon(0)
            ?: AppCompatResources.getDrawable(context, R.drawable.baseline_question_mark_24)!!

        // 4. Apply icon theme
        return applyThemeIfNeeded(context, systemIcon)
    }

    private fun applyThemeIfNeeded(context: Context, icon: Drawable): Drawable {
        return try {
            IconThemeManager.transform(context, icon)
        } catch (_: Exception) {
            icon
        }
    }

    override fun getRawInfo(): AppInfo {
        return app
    }

    override fun getUser(context: Context): UserHandle {
        return getUserFromId(app.user, context)
    }

    override fun isPrivate(): Boolean {
        return privateSpace
    }

    override fun isRemovable(): Boolean {
        return removable
    }

    override fun getAction(): Action {
        return AppAction(app)
    }


    companion object {
        fun fromAppInfo(appInfo: AppInfo, context: Context): DetailedAppInfo? {
            return appInfo.getLauncherActivityInfo(context)?.let {
                DetailedAppInfo(it, it.user == getPrivateSpaceUser(context), context)
            }
        }
    }
}