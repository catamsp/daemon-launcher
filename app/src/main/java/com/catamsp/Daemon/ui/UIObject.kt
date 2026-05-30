package com.catamsp.Daemon.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Resources
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.TextView
import android.graphics.Paint
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import androidx.appcompat.app.AppCompatActivity
import com.catamsp.Daemon.preferences.LauncherPreferences
import com.catamsp.Daemon.preferences.theme.Background
import com.catamsp.Daemon.preferences.theme.Font

/**
 * An interface implemented by every [Activity], Fragment etc. in Launcher.
 * It handles themes and window flags - a useful abstraction as it is the same everywhere.
 */
@Suppress("deprecation") // FLAG_FULLSCREEN is required to support API level < 30
fun setWindowFlags(window: Window, homeScreen: Boolean) {
    // Display notification bar
    if (LauncherPreferences.display().hideStatusBar())
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
    else window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

    // Screen Timeout
    if (LauncherPreferences.display().screenTimeoutDisabled())
        window.setFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
    else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    if (!homeScreen) {
        LauncherPreferences.theme().background().applyToWindow(window)

    }

}


interface UIObject {
    var ignoreAutoClose: Boolean
        get() = false
        set(_) {}

    fun onCreate() {
        if (this !is Activity) {
            return
        }
        setWindowFlags(window, isHomeScreen())

        if (!LauncherPreferences.display().rotateScreen()) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_NOSENSOR
        }
    }

    fun onStart() {
        setOnClicks()
        adjustLayout()
    }

    fun modifyTheme(theme: Resources.Theme): Resources.Theme {
        LauncherPreferences.theme().colorTheme().applyToTheme(
            theme,
            LauncherPreferences.theme().textShadow()
        )

        if (isHomeScreen()) {
            Background.TRANSPARENT.applyToTheme(theme)
        } else {
            LauncherPreferences.theme().background().applyToTheme(theme)
        }
        
        val fontName = LauncherPreferences.theme().font()
        Font.entries.find { it.name == fontName }?.applyToTheme(theme)

        return theme
    }

    fun setOnClicks() {}
    fun adjustLayout() {}

    /**
     * Recursively applies a custom font to a view and all its children.
     * Defaults to the global UI font if no fontName is provided.
     */
    fun applyFont(view: View?, fontName: String = LauncherPreferences.theme().font()) {
        if (view == null) return
        val tf = Font.getTypeface(view.context, fontName)

        if (view is TextView) {
            view.typeface = tf
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                applyFont(view.getChildAt(i), fontName)
            }
        }
    }

    /**
     * Specifically targets a Toolbar to apply a custom font to its title.
     */
    fun applyFontToToolbar(toolbar: androidx.appcompat.widget.Toolbar?, fontName: String = LauncherPreferences.theme().font()) {
        if (toolbar == null) return
        val tf = Font.getTypeface(toolbar.context, fontName)
        
        for (i in 0 until toolbar.childCount) {
            val child = toolbar.getChildAt(i)
            if (child is TextView) {
                child.typeface = tf
            }
        }
    }

    /**
     * Specifically targets the internal TextView of a SearchView to apply custom fonts.
     */
    fun applyFontToSearchView(searchView: View?) {
        if (searchView == null) return
        val fontName = LauncherPreferences.theme().font()
        val tf = Font.getTypeface(searchView.context, fontName)

        val searchText = searchView.findViewById<TextView>(androidx.appcompat.R.id.search_src_text)
        searchText?.typeface = tf
    }

    /**
     * Applies a custom font to all items in a Menu (e.g. PopupMenu).
     */
    fun applyFontToMenu(context: android.content.Context, menu: android.view.Menu?, fontName: String = LauncherPreferences.theme().font()) {
        if (menu == null) return
        val tf = Font.getTypeface(context, fontName)
        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            val span = SpannableString(item.title)
            span.setSpan(CustomTypefaceSpan(tf), 0, span.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            item.title = span
        }
    }

    fun isHomeScreen(): Boolean {
        return false
    }


    @Suppress("DEPRECATION")
    fun hideNavigationBar() {
        if (this !is Activity) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.apply {
                hide(WindowInsets.Type.navigationBars())
                systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // Try to hide the navigation bar but do not hide the status bar
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_IMMERSIVE
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        }
    }

    fun useSoftInputResizeWorkaround(container: View) {
        if (this !is Activity) {
            return
        }
        // android:windowSoftInputMode="adjustResize" doesn't work in full screen.
        // workaround from https://stackoverflow.com/a/57623505
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            this.window.decorView.viewTreeObserver.addOnGlobalLayoutListener {
                val r = Rect()
                window.decorView.getWindowVisibleDisplayFrame(r)
                val height: Int =
                    container.context.resources.displayMetrics.heightPixels
                val diff = height - r.bottom
                if (diff != 0 &&
                    LauncherPreferences.display().hideStatusBar()
                ) {
                    if (container.paddingBottom != diff) {
                        container.setPadding(0, 0, 0, diff)
                    }
                } else {
                    if (container.paddingBottom != 0) {
                        container.setPadding(0, 0, 0, 0)
                    }
                }
            }
        }
    }

}

class CustomTypefaceSpan(private val typeface: android.graphics.Typeface) : MetricAffectingSpan() {
    override fun updateDrawState(ds: TextPaint) {
        applyCustomTypeFace(ds, typeface)
    }

    override fun updateMeasureState(paint: TextPaint) {
        applyCustomTypeFace(paint, typeface)
    }

    private fun applyCustomTypeFace(paint: Paint, tf: android.graphics.Typeface) {
        paint.typeface = tf
    }
}

abstract class UIObjectActivity : AppCompatActivity(), UIObject {
    override var ignoreAutoClose: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super<AppCompatActivity>.onCreate(savedInstanceState)
        super<UIObject>.onCreate()
    }

    override fun onStart() {
        super<AppCompatActivity>.onStart()
        super<UIObject>.onStart()
    }

    override fun onResume() {
        super.onResume()
        ignoreAutoClose = false
    }

    override fun onPause() {
        super.onPause()
        if (!ignoreAutoClose) {
            finish()
        }
    }

    override fun getTheme(): Resources.Theme? {
        return modifyTheme(super.getTheme())
    }
}
