package com.catamsp.Daemon.widgets

import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.DisplayMetrics
import androidx.appcompat.content.res.AppCompatResources
import com.catamsp.Daemon.R

sealed class LauncherWidgetProvider(
    val label: CharSequence?,
    val description: CharSequence?,
    val icon: Drawable?,
    val previewImage: Drawable?
)

class LauncherAppWidgetProvider(
    val info: AppWidgetProviderInfo,
    label: CharSequence?,
    description: CharSequence?,
    icon: Drawable?,
    previewImage: Drawable?,
) : LauncherWidgetProvider(label, description, icon, previewImage) {

    constructor(info: AppWidgetProviderInfo, context: Context) : this(
        info,
        info.loadLabel(context.packageManager),
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            info.loadDescription(context)
        } else {
            null
        },
        info.loadIcon(context, DisplayMetrics.DENSITY_DEFAULT),
        info.loadPreviewImage(context, DisplayMetrics.DENSITY_DEFAULT)
    )
}

class LauncherClockWidgetProvider(context: Context) : LauncherWidgetProvider(
    context.getString(R.string.widget_clock_label),
    context.getString(R.string.widget_clock_description),
    AppCompatResources.getDrawable(context, R.drawable.baseline_clock_24),
    null
)

class LauncherGlobeWidgetProvider(context: Context) : LauncherWidgetProvider(
    context.getString(R.string.widget_globe_label),
    context.getString(R.string.widget_globe_description),
    AppCompatResources.getDrawable(context, R.drawable.baseline_apps_24),
    null
)

class LauncherAppIconWidgetProvider(context: Context) : LauncherWidgetProvider(
    context.getString(R.string.widget_app_icon_label),
    context.getString(R.string.widget_app_icon_description),
    AppCompatResources.getDrawable(context, R.drawable.baseline_apps_24),
    null
)

class LauncherFolderWidgetProvider(context: Context) : LauncherWidgetProvider(
    context.getString(R.string.widget_folder_label),
    context.getString(R.string.widget_folder_description),
    AppCompatResources.getDrawable(context, R.drawable.baseline_menu_24),
    null
)