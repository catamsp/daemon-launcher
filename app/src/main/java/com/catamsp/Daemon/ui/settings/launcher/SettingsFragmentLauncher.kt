package com.catamsp.Daemon.ui.settings.launcher

import android.app.Activity
import android.app.AlertDialog
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorInt
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import android.util.DisplayMetrics
import android.view.WindowManager
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
import com.catamsp.Daemon.ui.UIObjectActivity
import com.catamsp.Daemon.ui.settings.SettingsActivity
import com.catamsp.Daemon.ui.settings.SettingsItem
import com.catamsp.Daemon.ui.settings.SettingsRecyclerAdapter
import com.catamsp.Daemon.ui.widgets.manage.ManageWidgetPanelsActivity
import com.catamsp.Daemon.ui.widgets.manage.ManageWidgetsActivity

import com.catamsp.Daemon.widgets.wallpaper.WallpaperController

class SettingsFragmentLauncher : Fragment(), UIObject {

    private lateinit var binding: SettingsLauncherBinding
    private val adapter = SettingsRecyclerAdapter()
    
    private var isPickingVideo = false

    private val pickWallpaperLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            if (isPickingVideo) {
                WallpaperController.applyVideoWallpaper(requireActivity(), it) {
                    // Success
                }
            } else {
                WallpaperController.applyStaticWallpaper(
                    requireActivity(),
                    it,
                    onSuccess = {
                        Toast.makeText(requireContext(), "Wallpaper set!", Toast.LENGTH_SHORT).show()
                    },
                    onError = { e ->
                        Toast.makeText(requireContext(), "Failed to set wallpaper: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
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
        if (!isAdded) return
        val context = context ?: return
        
        val items = mutableListOf<SettingsItem>()
        val prefs = LauncherPreferences.getSharedPreferences()
        val fontSuffix = LauncherPreferences.theme().font().name

        // --- GENERAL ---
        items.add(SettingsItem.Header("hdr_general_$fontSuffix", getString(R.string.settings_general)))
        items.add(SettingsItem.Clickable("btn_home_$fontSuffix", getString(R.string.settings_general_choose_home_screen), null) {
            setDefaultHomeScreen(context, checkDefault = false)
        })

        // --- APPEARANCE ---
        items.add(SettingsItem.Header("hdr_app_$fontSuffix", getString(R.string.settings_launcher_section_appearance)))
        items.add(SettingsItem.Clickable("btn_wallpaper_$fontSuffix", getString(R.string.settings_theme_wallpaper), "Static or Video") {
            (activity as? UIObjectActivity)?.ignoreAutoClose = true
            val options = arrayOf("Static Photo", "Video Wallpaper")
            AlertDialog.Builder(context, R.style.AlertDialogCustom)
                .setTitle("Choose Wallpaper Mode")
                .setItems(options) { dialog, which ->
                    when (which) {
                        0 -> {
                            isPickingVideo = false
                            pickWallpaperLauncher.launch("image/*")
                        }
                        1 -> {
                            isPickingVideo = true
                            pickWallpaperLauncher.launch("video/*")
                        }
                    }
                    dialog.dismiss()
                }
                .setOnCancelListener {
                    (activity as? UIObjectActivity)?.ignoreAutoClose = false
                }
                .show()
        })

        val activity = requireActivity() as? SettingsActivity

        val themes = ColorTheme.entries.filter { it.isAvailable() }
        items.add(SettingsItem.Clickable("btn_theme_$fontSuffix", getString(R.string.settings_theme_color_theme), "Current: ${LauncherPreferences.theme().colorTheme().getLabel(context)}") {
            activity?.showSelectionCarousel("btn_theme", themes.indexOf(LauncherPreferences.theme().colorTheme()), themes.map { it.getLabel(context) }) { index: Int ->
                prefs.edit().putString(LauncherPreferences.theme().keys().colorTheme(), themes[index].name).apply()
            }
        })

        val fonts = Font.entries
        items.add(SettingsItem.Clickable("btn_font_$fontSuffix", getString(R.string.settings_theme_font), "Current: ${LauncherPreferences.theme().font().name.lowercase().replaceFirstChar { it.uppercase() }}") {
            activity?.showSelectionCarousel("btn_font", fonts.indexOf(LauncherPreferences.theme().font()), fonts.map { it.name.lowercase().replaceFirstChar { it.uppercase() } }) { index: Int ->
                prefs.edit().putString(LauncherPreferences.theme().keys().font(), fonts[index].name).apply()
            }
        })

        items.add(SettingsItem.Toggle("tgl_shadow_$fontSuffix", getString(R.string.settings_theme_text_shadow), null, null, LauncherPreferences.theme().textShadow()) {
            prefs.edit().putBoolean(LauncherPreferences.theme().keys().textShadow(), it).apply()
        })

        val bgs = Background.entries
        items.add(SettingsItem.Clickable("btn_bg_$fontSuffix", getString(R.string.settings_theme_background), "Current: ${LauncherPreferences.theme().background().name.lowercase().replaceFirstChar { it.uppercase() }}") {
            activity?.showSelectionCarousel("btn_bg", bgs.indexOf(LauncherPreferences.theme().background()), bgs.map { it.name.lowercase().replaceFirstChar { it.uppercase() } }) { index: Int ->
                prefs.edit().putString(LauncherPreferences.theme().keys().background(), bgs[index].name).apply()
            }
        })

        items.add(SettingsItem.Toggle("tgl_mono_$fontSuffix", getString(R.string.settings_theme_monochrome_icons), null, null, LauncherPreferences.theme().monochromeIcons()) {
            prefs.edit().putBoolean(LauncherPreferences.theme().keys().monochromeIcons(), it).apply()
        })

        // --- FUNCTIONALITY ---
        items.add(SettingsItem.Header("hdr_func_$fontSuffix", getString(R.string.settings_launcher_section_functionality)))
        items.add(SettingsItem.Toggle("tgl_auto_launch_$fontSuffix", getString(R.string.settings_functionality_auto_launch), getString(R.string.settings_functionality_auto_launch_summary), null, LauncherPreferences.functionality().searchAutoLaunch()) {
            prefs.edit().putBoolean(LauncherPreferences.functionality().keys().searchAutoLaunch(), it).apply()
        })
        items.add(SettingsItem.Toggle("tgl_web_search_$fontSuffix", getString(R.string.settings_functionality_search_web), getString(R.string.settings_functionality_search_web_summary), null, LauncherPreferences.functionality().searchWeb()) {
            prefs.edit().putBoolean(LauncherPreferences.functionality().keys().searchWeb(), it).apply()
        })
        items.add(SettingsItem.Toggle("tgl_auto_kb_$fontSuffix", getString(R.string.settings_functionality_auto_keyboard), null, null, LauncherPreferences.functionality().searchAutoOpenKeyboard()) {
            prefs.edit().putBoolean(LauncherPreferences.functionality().keys().searchAutoOpenKeyboard(), it).apply()
        })
        items.add(SettingsItem.Toggle("tgl_close_kb_$fontSuffix", getString(R.string.settings_functionality_auto_close_keyboard), null, null, LauncherPreferences.functionality().searchAutoCloseKeyboard()) {
            prefs.edit().putBoolean(LauncherPreferences.functionality().keys().searchAutoCloseKeyboard(), it).apply()
        })
        items.add(SettingsItem.Clickable("btn_lock_$fontSuffix", getString(R.string.settings_actions_lock_method), null) {
            LockMethod.chooseMethod(context)
        })

        // --- APPS ---
        items.add(SettingsItem.Header("hdr_apps_$fontSuffix", getString(R.string.settings_launcher_section_apps)))
        items.add(SettingsItem.Clickable("btn_hidden_$fontSuffix", getString(R.string.settings_apps_hidden), null) {
            openAppsList(context, favorite = false, hidden = true)
        })
        items.add(SettingsItem.Toggle("tgl_hide_bound_$fontSuffix", getString(R.string.settings_apps_hide_bound_apps), null, null, LauncherPreferences.apps().hideBoundApps()) {
            prefs.edit().putBoolean(LauncherPreferences.apps().keys().hideBoundApps(), it).apply()
        })
        items.add(SettingsItem.Toggle("tgl_hide_paused_$fontSuffix", getString(R.string.settings_apps_hide_paused_apps), null, null, LauncherPreferences.apps().hidePausedApps()) {
            prefs.edit().putBoolean(LauncherPreferences.apps().keys().hidePausedApps(), it).apply()
        })
        items.add(SettingsItem.Toggle("tgl_hide_private_$fontSuffix", getString(R.string.settings_apps_hide_private_space_apps), null, null, LauncherPreferences.apps().hidePrivateSpaceApps()) {
            prefs.edit().putBoolean(LauncherPreferences.apps().keys().hidePrivateSpaceApps(), it).apply()
        })

        val layouts = ListLayout.entries
        items.add(SettingsItem.Clickable("btn_list_layout_$fontSuffix", getString(R.string.settings_list_layout), "Current: ${LauncherPreferences.list().layout().name.lowercase().replaceFirstChar { it.uppercase() }}") {
            activity?.showSelectionCarousel("btn_list_layout", layouts.indexOf(LauncherPreferences.list().layout()), layouts.map { it.name.lowercase().replaceFirstChar { it.uppercase() } }) { index: Int ->
                prefs.edit().putString(LauncherPreferences.list().keys().layout(), layouts[index].name).apply()
            }
        })

        val formats = AppNameFormat.entries
        items.add(SettingsItem.Clickable("btn_name_format_$fontSuffix", getString(R.string.settings_list_app_name_format), "Current: ${LauncherPreferences.list().appNameFormat().name.lowercase().replaceFirstChar { it.uppercase() }}") {
            activity?.showSelectionCarousel("btn_name_format", formats.indexOf(LauncherPreferences.list().appNameFormat()), formats.map { it.name.lowercase().replaceFirstChar { it.uppercase() } }) { index: Int ->
                prefs.edit().putString(LauncherPreferences.list().keys().appNameFormat(), formats[index].name).apply()
            }
        })

        items.add(SettingsItem.Toggle("tgl_rev_layout_$fontSuffix", getString(R.string.settings_list_reverse_layout), null, null, LauncherPreferences.list().reverseLayout()) {
            prefs.edit().putBoolean(LauncherPreferences.list().keys().reverseLayout(), it).apply()
        })

        // --- DISPLAY ---
        items.add(SettingsItem.Header("hdr_display_$fontSuffix", getString(R.string.settings_launcher_section_display)))
        items.add(SettingsItem.Toggle("tgl_rotate_$fontSuffix", getString(R.string.settings_display_rotate_screen), null, null, LauncherPreferences.display().rotateScreen()) {
            prefs.edit().putBoolean(LauncherPreferences.display().keys().rotateScreen(), it).apply()
        })
        items.add(SettingsItem.Toggle("tgl_timeout_$fontSuffix", getString(R.string.settings_display_screen_timeout_disabled), null, null, LauncherPreferences.display().screenTimeoutDisabled()) {
            prefs.edit().putBoolean(LauncherPreferences.display().keys().screenTimeoutDisabled(), it).apply()
        })
        items.add(SettingsItem.Toggle("tgl_status_$fontSuffix", getString(R.string.settings_display_hide_status_bar), null, null, LauncherPreferences.display().hideStatusBar()) {
            prefs.edit().putBoolean(LauncherPreferences.display().keys().hideStatusBar(), it).apply()
        })
        items.add(SettingsItem.Toggle("tgl_nav_$fontSuffix", getString(R.string.settings_display_hide_navigation_bar), null, null, LauncherPreferences.display().hideNavigationBar()) {
            prefs.edit().putBoolean(LauncherPreferences.display().keys().hideNavigationBar(), it).apply()
        })

        adapter.submitList(items)
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
