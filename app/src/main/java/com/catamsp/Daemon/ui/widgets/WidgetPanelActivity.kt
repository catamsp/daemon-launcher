package com.catamsp.Daemon.ui.widgets

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.view.View
import androidx.core.view.ViewCompat
import com.catamsp.Daemon.Application
import com.catamsp.Daemon.R
import com.catamsp.Daemon.databinding.ActivityWidgetPanelBinding
import com.catamsp.Daemon.preferences.LauncherPreferences
import com.catamsp.Daemon.ui.HomeActivity
import com.catamsp.Daemon.ui.UIObject
import com.catamsp.Daemon.ui.util.LauncherGestureActivity
import com.catamsp.Daemon.ui.widgets.manage.EXTRA_PANEL_ID
import com.catamsp.Daemon.widgets.WidgetPanel

class WidgetPanelActivity : LauncherGestureActivity(), UIObject {
    var binding: ActivityWidgetPanelBinding? = null

    var widgetPanelId: Int = WidgetPanel.HOME.id

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val newId = intent.getIntExtra(EXTRA_PANEL_ID, WidgetPanel.HOME.id)
        if (newId == WidgetPanel.HOME.id) {
            finish()
            return
        }
        widgetPanelId = newId
        binding?.widgetPanelWidgetContainer?.widgetPanelId = widgetPanelId
        binding?.widgetPanelWidgetContainer?.updateWidgets(
            this,
            LauncherPreferences.widgets().widgets()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super<LauncherGestureActivity>.onCreate(savedInstanceState)
        super<UIObject>.onCreate()
        
        val id = intent.getIntExtra(EXTRA_PANEL_ID, WidgetPanel.HOME.id)
        if (id == WidgetPanel.HOME.id) {
            finish()
            return
        }
        widgetPanelId = id

        val b = ActivityWidgetPanelBinding.inflate(layoutInflater)
        this.binding = b
        setContentView(b.root)

        // The widget container should extend below the status and navigation bars,
        // so let's set an empty WindowInsetsListener to prevent it from being moved.
        ViewCompat.setOnApplyWindowInsetsListener(b.root) { _, windowInsets ->
            windowInsets
        }

        b.widgetPanelWidgetContainer.widgetPanelId = widgetPanelId
        b.widgetPanelWidgetContainer.updateWidgets(
            this,
            LauncherPreferences.widgets().widgets()
        )
    }

    override fun getTheme(): Resources.Theme {
        val mTheme = modifyTheme(super.getTheme())
        mTheme.applyStyle(R.style.backgroundWallpaper, true)
        LauncherPreferences.clock().font().applyToTheme(mTheme)
        LauncherPreferences.theme().colorTheme().applyToTheme(
            mTheme,
            LauncherPreferences.theme().textShadow()
        )

        return mTheme
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus && LauncherPreferences.display().hideNavigationBar()) {
            hideNavigationBar()
        }
    }

    override fun onStart() {
        super<LauncherGestureActivity>.onStart()
        super<UIObject>.onStart()
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
        (application as Application).appWidgetHost.startListening()
    }

    override fun getRootView(): View? {
        return binding?.root
    }

    override fun handleBack() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
        finish()
    }

    override fun isHomeScreen(): Boolean {
        return true
    }
}