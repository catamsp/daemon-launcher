package com.catamsp.Daemon.ui.settings.launcher

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.catamsp.Daemon.R
import com.catamsp.Daemon.actions.lock.LockMethod
import com.catamsp.Daemon.actions.openAppsList
import com.catamsp.Daemon.databinding.SettingsLauncherBinding
import com.catamsp.Daemon.preferences.LauncherPreferences
import com.catamsp.Daemon.preferences.theme.Background
import com.catamsp.Daemon.preferences.theme.ColorTheme
import com.catamsp.Daemon.preferences.theme.Font
import com.catamsp.Daemon.preferences.list.ListLayout
import com.catamsp.Daemon.preferences.list.AppNameFormat
import com.catamsp.Daemon.setDefaultHomeScreen
import com.catamsp.Daemon.ui.UIObject
import com.catamsp.Daemon.ui.settings.SettingsItem
import com.catamsp.Daemon.ui.settings.SettingsRecyclerAdapter
import com.catamsp.Daemon.ui.widgets.manage.ManageWidgetPanelsActivity
import com.catamsp.Daemon.ui.widgets.manage.ManageWidgetsActivity

class SettingsFragmentLauncher : Fragment(), UIObject {

    private lateinit var binding: SettingsLauncherBinding
    private val adapter = SettingsRecyclerAdapter()

    private val sharedPreferencesListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            refreshList()
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = SettingsLauncherBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.launcherRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.launcherRecyclerView.adapter = adapter
        refreshList()
    }

    override fun onStart() {
        super<Fragment>.onStart()
        LauncherPreferences.getSharedPreferences()
            .registerOnSharedPreferenceChangeListener(sharedPreferencesListener)
        super<UIObject>.onStart()
    }

    override fun onPause() {
        LauncherPreferences.getSharedPreferences()
            .unregisterOnSharedPreferenceChangeListener(sharedPreferencesListener)
        super.onPause()
    }

    private fun refreshList() {
        val items = mutableListOf<SettingsItem>()
        val prefs = LauncherPreferences.getSharedPreferences()
        val context = requireContext()

        // --- GENERAL ---
        items.add(SettingsItem.Header("hdr_general", getString(R.string.settings_general)))
        items.add(SettingsItem.Clickable("btn_home", getString(R.string.settings_general_choose_home_screen), null) {
            setDefaultHomeScreen(context, checkDefault = false)
        })

        // --- APPEARANCE ---
        items.add(SettingsItem.Header("hdr_app", getString(R.string.settings_launcher_section_appearance)))
        items.add(SettingsItem.Clickable("btn_wallpaper", getString(R.string.settings_theme_wallpaper), null) {
            val intent = Intent(Intent.ACTION_SET_WALLPAPER)
                .putExtra("com.android.wallpaper.LAUNCH_SOURCE", "app_launched_launcher")
                .putExtra("com.android.launcher3.WALLPAPER_FLAVOR", "focus_wallpaper")
            startActivity(intent)
        })

        val themes = ColorTheme.entries.filter { it.isAvailable() }
        items.add(SettingsItem.Clickable("btn_theme", getString(R.string.settings_theme_color_theme), "Current: ${LauncherPreferences.theme().colorTheme().getLabel(context)}") {
            showSingleChoiceDialog(getString(R.string.settings_theme_color_theme), 
                themes.map { it.getLabel(context) }.toTypedArray(),
                themes.indexOf(LauncherPreferences.theme().colorTheme())
            ) { index ->
                prefs.edit().putString(LauncherPreferences.theme().keys().colorTheme(), themes[index].name).apply()
            }
        })

        val fonts = Font.entries
        items.add(SettingsItem.Clickable("btn_font", getString(R.string.settings_theme_font), "Current: ${LauncherPreferences.theme().font().name.lowercase().replaceFirstChar { it.uppercase() }}") {
            showSingleChoiceDialog(getString(R.string.settings_theme_font),
                fonts.map { it.name.lowercase().replaceFirstChar { it.uppercase() } }.toTypedArray(),
                fonts.indexOf(LauncherPreferences.theme().font())
            ) { index ->
                prefs.edit().putString(LauncherPreferences.theme().keys().font(), fonts[index].name).apply()
            }
        })

        items.add(SettingsItem.Toggle("tgl_shadow", getString(R.string.settings_theme_text_shadow), null, null, LauncherPreferences.theme().textShadow()) {
            prefs.edit().putBoolean(LauncherPreferences.theme().keys().textShadow(), it).apply()
        })

        val bgs = Background.entries
        items.add(SettingsItem.Clickable("btn_bg", getString(R.string.settings_theme_background), "Current: ${LauncherPreferences.theme().background().name.lowercase().replaceFirstChar { it.uppercase() }}") {
            showSingleChoiceDialog(getString(R.string.settings_theme_background),
                bgs.map { it.name.lowercase().replaceFirstChar { it.uppercase() } }.toTypedArray(),
                bgs.indexOf(LauncherPreferences.theme().background())
            ) { index ->
                prefs.edit().putString(LauncherPreferences.theme().keys().background(), bgs[index].name).apply()
            }
        })

        items.add(SettingsItem.Toggle("tgl_mono", getString(R.string.settings_theme_monochrome_icons), null, null, LauncherPreferences.theme().monochromeIcons()) {
            prefs.edit().putBoolean(LauncherPreferences.theme().keys().monochromeIcons(), it).apply()
        })

        items.add(SettingsItem.Toggle("tgl_anim", getString(R.string.settings_theme_animations), null, null, LauncherPreferences.theme().animations()) {
            prefs.edit().putBoolean(LauncherPreferences.theme().keys().animations(), it).apply()
        })

        // --- DATE & TIME ---
        items.add(SettingsItem.Header("hdr_clock", getString(R.string.settings_launcher_section_date_time)))
        
        items.add(SettingsItem.Clickable("btn_clock_font", getString(R.string.settings_clock_font), "Current: ${LauncherPreferences.clock().font().name.lowercase().replaceFirstChar { it.uppercase() }}") {
            showSingleChoiceDialog(getString(R.string.settings_clock_font),
                fonts.map { it.name.lowercase().replaceFirstChar { it.uppercase() } }.toTypedArray(),
                fonts.indexOf(LauncherPreferences.clock().font())
            ) { index ->
                prefs.edit().putString(LauncherPreferences.clock().keys().font(), fonts[index].name).apply()
            }
        })

        items.add(SettingsItem.Clickable("btn_clock_color", getString(R.string.settings_clock_color), "Hex: #%08X".format(LauncherPreferences.clock().color())) {
            showColorPickerDialog(LauncherPreferences.clock().color()) { color ->
                prefs.edit().putInt(LauncherPreferences.clock().keys().color(), color).apply()
            }
        })

        items.add(SettingsItem.Toggle("tgl_clock_loc", getString(R.string.settings_clock_localized), null, null, LauncherPreferences.clock().localized()) {
            prefs.edit().putBoolean(LauncherPreferences.clock().keys().localized(), it).apply()
        })
        items.add(SettingsItem.Toggle("tgl_time", getString(R.string.settings_clock_time_visible), null, null, LauncherPreferences.clock().timeVisible()) {
            prefs.edit().putBoolean(LauncherPreferences.clock().keys().timeVisible(), it).apply()
        })
        items.add(SettingsItem.Toggle("tgl_seconds", getString(R.string.settings_clock_show_seconds), null, null, LauncherPreferences.clock().showSeconds()) {
            prefs.edit().putBoolean(LauncherPreferences.clock().keys().showSeconds(), it).apply()
        })
        items.add(SettingsItem.Toggle("tgl_date", getString(R.string.settings_clock_date_visible), null, null, LauncherPreferences.clock().dateVisible()) {
            prefs.edit().putBoolean(LauncherPreferences.clock().keys().dateVisible(), it).apply()
        })
        items.add(SettingsItem.Toggle("tgl_flip", getString(R.string.settings_clock_flip_date_time), null, null, LauncherPreferences.clock().flipDateTime()) {
            prefs.edit().putBoolean(LauncherPreferences.clock().keys().flipDateTime(), it).apply()
        })

        // --- FUNCTIONALITY ---
        items.add(SettingsItem.Header("hdr_func", getString(R.string.settings_launcher_section_functionality)))
        items.add(SettingsItem.Toggle("tgl_auto_launch", getString(R.string.settings_functionality_auto_launch), getString(R.string.settings_functionality_auto_launch_summary), null, LauncherPreferences.functionality().searchAutoLaunch()) {
            prefs.edit().putBoolean(LauncherPreferences.functionality().keys().searchAutoLaunch(), it).apply()
        })
        items.add(SettingsItem.Toggle("tgl_web_search", getString(R.string.settings_functionality_search_web), getString(R.string.settings_functionality_search_web_summary), null, LauncherPreferences.functionality().searchWeb()) {
            prefs.edit().putBoolean(LauncherPreferences.functionality().keys().searchWeb(), it).apply()
        })
        items.add(SettingsItem.Toggle("tgl_auto_kb", getString(R.string.settings_functionality_auto_keyboard), null, null, LauncherPreferences.functionality().searchAutoOpenKeyboard()) {
            prefs.edit().putBoolean(LauncherPreferences.functionality().keys().searchAutoOpenKeyboard(), it).apply()
        })
        items.add(SettingsItem.Toggle("tgl_close_kb", getString(R.string.settings_functionality_auto_close_keyboard), null, null, LauncherPreferences.functionality().searchAutoCloseKeyboard()) {
            prefs.edit().putBoolean(LauncherPreferences.functionality().keys().searchAutoCloseKeyboard(), it).apply()
        })
        items.add(SettingsItem.Clickable("btn_lock", getString(R.string.settings_actions_lock_method), null) {
            LockMethod.chooseMethod(context)
        })

        // --- GESTURES ---
        items.add(SettingsItem.Header("hdr_gestures", getString(R.string.settings_launcher_section_gestures)))
        items.add(SettingsItem.Toggle("tgl_double_swipe", getString(R.string.settings_enabled_gestures_double_swipe), getString(R.string.settings_enabled_gestures_double_swipe_summary), null, LauncherPreferences.enabled_gestures().doubleSwipe()) {
            prefs.edit().putBoolean(LauncherPreferences.enabled_gestures().keys().doubleSwipe(), it).apply()
        })
        items.add(SettingsItem.Toggle("tgl_edge_swipe", getString(R.string.settings_enabled_gestures_edge_swipe), getString(R.string.settings_enabled_gestures_edge_swipe_summary), null, LauncherPreferences.enabled_gestures().edgeSwipe()) {
            prefs.edit().putBoolean(LauncherPreferences.enabled_gestures().keys().edgeSwipe(), it).apply()
        })
        items.add(SettingsItem.Slider("sld_edge_width", getString(R.string.settings_enabled_gestures_edge_swipe_edge_width), null, LauncherPreferences.enabled_gestures().edgeSwipeEdgeWidth(), 0, 33) {
            prefs.edit().putInt(LauncherPreferences.enabled_gestures().keys().edgeSwipeEdgeWidth(), it).apply()
        })
        items.add(SettingsItem.Toggle("tgl_diag_swipe", getString(R.string.settings_enabled_gestures_diagonal_swipe), null, null, LauncherPreferences.enabled_gestures().diagonalSwipe()) {
            prefs.edit().putBoolean(LauncherPreferences.enabled_gestures().keys().diagonalSwipe(), it).apply()
        })

        // --- APPS ---
        items.add(SettingsItem.Header("hdr_apps", getString(R.string.settings_launcher_section_apps)))
        items.add(SettingsItem.Clickable("btn_hidden", getString(R.string.settings_apps_hidden), null) {
            openAppsList(context, favorite = false, hidden = true)
        })
        items.add(SettingsItem.Toggle("tgl_hide_bound", getString(R.string.settings_apps_hide_bound_apps), null, null, LauncherPreferences.apps().hideBoundApps()) {
            prefs.edit().putBoolean(LauncherPreferences.apps().keys().hideBoundApps(), it).apply()
        })
        items.add(SettingsItem.Toggle("tgl_hide_paused", getString(R.string.settings_apps_hide_paused_apps), null, null, LauncherPreferences.apps().hidePausedApps()) {
            prefs.edit().putBoolean(LauncherPreferences.apps().keys().hidePausedApps(), it).apply()
        })
        items.add(SettingsItem.Toggle("tgl_hide_private", getString(R.string.settings_apps_hide_private_space_apps), null, null, LauncherPreferences.apps().hidePrivateSpaceApps()) {
            prefs.edit().putBoolean(LauncherPreferences.apps().keys().hidePrivateSpaceApps(), it).apply()
        })

        val layouts = ListLayout.entries
        items.add(SettingsItem.Clickable("btn_list_layout", getString(R.string.settings_list_layout), "Current: ${LauncherPreferences.list().layout().name.lowercase().replaceFirstChar { it.uppercase() }}") {
            showSingleChoiceDialog(getString(R.string.settings_list_layout),
                layouts.map { it.name.lowercase().replaceFirstChar { it.uppercase() } }.toTypedArray(),
                layouts.indexOf(LauncherPreferences.list().layout())
            ) { index ->
                prefs.edit().putString(LauncherPreferences.list().keys().layout(), layouts[index].name).apply()
            }
        })

        val formats = AppNameFormat.entries
        items.add(SettingsItem.Clickable("btn_name_format", getString(R.string.settings_list_app_name_format), "Current: ${LauncherPreferences.list().appNameFormat().name.lowercase().replaceFirstChar { it.uppercase() }}") {
            showSingleChoiceDialog(getString(R.string.settings_list_app_name_format),
                formats.map { it.name.lowercase().replaceFirstChar { it.uppercase() } }.toTypedArray(),
                formats.indexOf(LauncherPreferences.list().appNameFormat())
            ) { index ->
                prefs.edit().putString(LauncherPreferences.list().keys().appNameFormat(), formats[index].name).apply()
            }
        })

        items.add(SettingsItem.Toggle("tgl_rev_layout", getString(R.string.settings_list_reverse_layout), null, null, LauncherPreferences.list().reverseLayout()) {
            prefs.edit().putBoolean(LauncherPreferences.list().keys().reverseLayout(), it).apply()
        })

        // --- WIDGETS ---
        items.add(SettingsItem.Header("hdr_widgets", getString(R.string.settings_launcher_section_widgets)))
        items.add(SettingsItem.Clickable("btn_widgets", getString(R.string.settings_widgets_widgets), null) {
            startActivity(Intent(activity, ManageWidgetsActivity::class.java))
        })
        items.add(SettingsItem.Clickable("btn_panels", getString(R.string.settings_widgets_custom_panels), null) {
            startActivity(Intent(activity, ManageWidgetPanelsActivity::class.java))
        })

        // --- DISPLAY ---
        items.add(SettingsItem.Header("hdr_display", getString(R.string.settings_launcher_section_display)))
        items.add(SettingsItem.Toggle("tgl_rotate", getString(R.string.settings_display_rotate_screen), null, null, LauncherPreferences.display().rotateScreen()) {
            prefs.edit().putBoolean(LauncherPreferences.display().keys().rotateScreen(), it).apply()
        })
        items.add(SettingsItem.Toggle("tgl_timeout", getString(R.string.settings_display_screen_timeout_disabled), null, null, LauncherPreferences.display().screenTimeoutDisabled()) {
            prefs.edit().putBoolean(LauncherPreferences.display().keys().screenTimeoutDisabled(), it).apply()
        })
        items.add(SettingsItem.Toggle("tgl_status", getString(R.string.settings_display_hide_status_bar), null, null, LauncherPreferences.display().hideStatusBar()) {
            prefs.edit().putBoolean(LauncherPreferences.display().keys().hideStatusBar(), it).apply()
        })
        items.add(SettingsItem.Toggle("tgl_nav", getString(R.string.settings_display_hide_navigation_bar), null, null, LauncherPreferences.display().hideNavigationBar()) {
            prefs.edit().putBoolean(LauncherPreferences.display().keys().hideNavigationBar(), it).apply()
        })

        adapter.submitList(items)
    }

    private fun showSingleChoiceDialog(title: String, options: Array<String>, currentIndex: Int, onSelect: (Int) -> Unit) {
        AlertDialog.Builder(requireContext(), R.style.AlertDialogCustom)
            .setTitle(title)
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                onSelect(which)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun showColorPickerDialog(initialColor: Int, onColorSelected: (Int) -> Unit) {
        var currentColor = initialColor
        val context = requireContext()

        AlertDialog.Builder(context, R.style.AlertDialogCustom).apply {
            setView(R.layout.dialog_choose_color)
            setTitle(R.string.dialog_choose_color_title)
            setPositiveButton(android.R.string.ok) { _, _ ->
                onColorSelected(currentColor)
            }
            setNegativeButton(R.string.dialog_cancel, null)
        }.create().also { it.show() }.apply {
            val preview = findViewById<EditText>(R.id.dialog_select_color_preview)
            val red = findViewById<SeekBar>(R.id.dialog_select_color_seekbar_red)
            val green = findViewById<SeekBar>(R.id.dialog_select_color_seekbar_green)
            val blue = findViewById<SeekBar>(R.id.dialog_select_color_seekbar_blue)
            val alpha = findViewById<SeekBar>(R.id.dialog_select_color_seekbar_alpha)

            fun updateUI(updateText: Boolean) {
                preview?.setBackgroundColor(currentColor)
                val brightness = (currentColor.red * 0.299 + currentColor.green * 0.587 + currentColor.blue * 0.114)
                preview?.setTextColor(if (brightness > 150) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                if (updateText) preview?.setText("#%08X".format(currentColor))
                red?.progress = currentColor.red
                green?.progress = currentColor.green
                blue?.progress = currentColor.blue
                alpha?.progress = currentColor.alpha
            }

            updateUI(true)

            red?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { if(f) { currentColor = android.graphics.Color.argb(currentColor.alpha, p, currentColor.green, currentColor.blue); updateUI(true) } }
                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {}
            })
            green?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { if(f) { currentColor = android.graphics.Color.argb(currentColor.alpha, currentColor.red, p, currentColor.blue); updateUI(true) } }
                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {}
            })
            blue?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { if(f) { currentColor = android.graphics.Color.argb(currentColor.alpha, currentColor.red, currentColor.green, p); updateUI(true) } }
                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {}
            })
            alpha?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { if(f) { currentColor = android.graphics.Color.argb(p, currentColor.red, currentColor.green, currentColor.blue); updateUI(true) } }
                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {}
            })
        }
    }
}
