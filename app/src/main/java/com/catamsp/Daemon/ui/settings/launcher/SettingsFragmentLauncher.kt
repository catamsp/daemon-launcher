package com.catamsp.Daemon.ui.settings.launcher

import android.app.Activity
import android.app.AlertDialog
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.catamsp.Daemon.R
import com.catamsp.Daemon.databinding.SettingsLauncherBinding
import com.catamsp.Daemon.preferences.LauncherPreferences
import com.catamsp.Daemon.preferences.theme.Background
import com.catamsp.Daemon.preferences.theme.ColorTheme
import com.catamsp.Daemon.preferences.theme.Font
import com.catamsp.Daemon.preferences.theme.FontManager
import com.catamsp.Daemon.ui.UIObject
import com.catamsp.Daemon.ui.UIObjectActivity
import com.catamsp.Daemon.ui.list.AppListActivity
import com.catamsp.Daemon.ui.settings.SettingsActivity
import com.catamsp.Daemon.ui.settings.SettingsItem
import com.catamsp.Daemon.ui.settings.SettingsRecyclerAdapter
import com.catamsp.Daemon.ui.widgets.manage.ManageWidgetsActivity
import com.catamsp.Daemon.widgets.wallpaper.WallpaperController
import com.catamsp.Daemon.setDefaultHomeScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsFragmentLauncher : Fragment(), UIObject {

    private lateinit var binding: SettingsLauncherBinding
    private val adapter = SettingsRecyclerAdapter()
    
    private var isPickingVideo = false

    private val pickFontLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            if (!isAdded) return@let
            CoroutineScope(Dispatchers.IO).launch {
                val fontName = FontManager.importFont(requireContext(), it)
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    if (fontName != null) {
                        Toast.makeText(context, "Font imported!", Toast.LENGTH_SHORT).show()
                        refreshList()
                        // Automatically select and trigger the carousel for the new font
                        val prefs = LauncherPreferences.getSharedPreferences()
                        prefs.edit().putString(LauncherPreferences.theme().keys().font(), fontName).apply()
                    } else {
                        Toast.makeText(context, "Failed to import font. Make sure it is .ttf or .otf", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private val pickWallpaperLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            if (isPickingVideo) {
                WallpaperController.applyVideoWallpaper(requireActivity(), it) {
                    // Success
                }
            } else {
                WallpaperController.applyStaticWallpaper(requireActivity(), it, {
                    // Success
                }, {
                    Toast.makeText(context, "Failed to apply wallpaper", Toast.LENGTH_SHORT).show()
                })
            }
        }
    }

private val sharedPreferencesListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, prefKey ->
            if (prefKey == LauncherPreferences.theme().keys().font()) {
                refreshListWithFontUpdate()
            } else if (prefKey == LauncherPreferences.theme().keys().spacingDensity()) {
                refreshListWithSpacingUpdate()
            } else {
                refreshList()
            }
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

    private fun buildSettingsItems(): List<SettingsItem> {
        val items = mutableListOf<SettingsItem>()
        val context = context ?: return emptyList()
        val prefs = LauncherPreferences.getSharedPreferences()
        val activity = requireActivity() as? SettingsActivity

        // --- GENERAL ---
        items.add(SettingsItem.Header("hdr_general", getString(R.string.settings_general)))
        items.add(SettingsItem.Clickable("btn_home", getString(R.string.settings_general_choose_home_screen), null) {
            setDefaultHomeScreen(context, false)
        })

        // --- APPEARANCE ---
        items.add(SettingsItem.Header("hdr_app", getString(R.string.settings_launcher_section_appearance)))
        
        items.add(SettingsItem.Clickable("btn_wallpaper", getString(R.string.settings_theme_wallpaper), "Current: Tap to change") {
            // Use Binary Ribbon for wallpaper selection (Static vs Video)
            activity?.showBinaryRibbon(
                button1Text = "Static Image",
                button2Text = "Cinematic Video",
                onButton1Click = {
                    // CRITICAL: Set the safety lock so the settings menu doesn't close when the picker opens!
                    (activity as? UIObjectActivity)?.ignoreAutoClose = true
                    
                    // Launch Static Image Picker
                    isPickingVideo = false
                    pickWallpaperLauncher.launch("image/*")
                    activity.hideBinaryRibbon()
                },
                onButton2Click = {
                    // CRITICAL: Set the safety lock so the settings menu doesn't close when the picker opens!
                    (activity as? UIObjectActivity)?.ignoreAutoClose = true
                    
                    // Launch Video Picker
                    isPickingVideo = true
                    pickWallpaperLauncher.launch("video/*")
                    activity.hideBinaryRibbon()
                }
            )
        })

        val themes = ColorTheme.entries.filter { it.isAvailable() }
        items.add(SettingsItem.Clickable("btn_theme", getString(R.string.settings_theme_color_theme), "Current: ${LauncherPreferences.theme().colorTheme().getLabel(context)}") {
            activity?.showSelectionCarousel("btn_theme", themes.indexOf(LauncherPreferences.theme().colorTheme()), themes.map { it.getLabel(context) }) { index: Int ->
                prefs.edit().putString(LauncherPreferences.theme().keys().colorTheme(), themes[index].name).apply()
            }
        })

        // Build Dynamic Font List (Built-in + Custom)
        val builtInFonts = Font.entries.map { it.name }
        val customFonts = FontManager.getCustomFontNames(context)
        val allFonts = builtInFonts + customFonts

        val currentFontName = LauncherPreferences.theme().font()
        val currentFontLabel = try { FontManager.getDisplayName(currentFontName) } catch (e: Exception) { currentFontName }

        items.add(SettingsItem.Clickable(
            "btn_font", 
            getString(R.string.settings_theme_font), 
            "Current: $currentFontLabel",
            onRemove = { 
                // CRITICAL: Set ignoreAutoClose BEFORE launching to prevent immediate finish()
                (activity as? UIObjectActivity)?.ignoreAutoClose = true
                pickFontLauncher.launch(arrayOf("*/*")) 
            }
        ) {
            val displayNames = allFonts.map { FontManager.getDisplayName(it) }
            activity?.showSelectionCarousel("btn_font", allFonts.indexOf(currentFontName), displayNames, allFonts) { index: Int ->
                // CRITICAL: Refresh the lock inside the callback so it survives multiple selections!
                (activity as? UIObjectActivity)?.ignoreAutoClose = true
                prefs.edit().putString(LauncherPreferences.theme().keys().font(), allFonts[index]).apply()
            }
        })
        items.add(SettingsItem.Toggle("tgl_shadow", getString(R.string.settings_theme_text_shadow), null, null, LauncherPreferences.theme().textShadow()) {
            prefs.edit().putBoolean(LauncherPreferences.theme().keys().textShadow(), it).apply()
        })

        val bgs = Background.entries
        items.add(SettingsItem.Clickable("btn_bg", getString(R.string.settings_theme_background), "Current: ${LauncherPreferences.theme().background().name.lowercase().replaceFirstChar { it.uppercase() }}") {
            activity?.showSelectionCarousel("btn_bg", bgs.indexOf(LauncherPreferences.theme().background()), bgs.map { it.name.lowercase().replaceFirstChar { it.uppercase() } }) { index: Int ->
                prefs.edit().putString(LauncherPreferences.theme().keys().background(), bgs[index].name).apply()
            }
        })

        items.add(SettingsItem.Toggle("tgl_mono", getString(R.string.settings_theme_monochrome_icons), null, null, LauncherPreferences.theme().monochromeIcons()) {
            prefs.edit().putBoolean(LauncherPreferences.theme().keys().monochromeIcons(), it).apply()
        })

        // Spacing Density - Ternary Ribbon
        val spacingValues = arrayOf("compact", "default", "spacious")
        val spacingItems = arrayOf(
            getString(R.string.settings_theme_spacing_item_compact),
            getString(R.string.settings_theme_spacing_item_default),
            getString(R.string.settings_theme_spacing_item_spacious)
        )
        val currentSpacingIndex = spacingValues.indexOf(LauncherPreferences.theme().spacingDensity())
        items.add(SettingsItem.Clickable("btn_spacing", getString(R.string.settings_theme_spacing), "Current: ${spacingItems[currentSpacingIndex]}") {
            activity?.showTernaryRibbon(
                button1Text = getString(R.string.settings_theme_spacing_item_compact),
                button2Text = getString(R.string.settings_theme_spacing_item_default),
                button3Text = getString(R.string.settings_theme_spacing_item_spacious),
                onButton1Click = {
                    prefs.edit().putString(LauncherPreferences.theme().keys().spacingDensity(), "compact").apply()
                    activity?.applySpacingChanges()
                    activity.hideBinaryRibbon()
                },
                onButton2Click = {
                    prefs.edit().putString(LauncherPreferences.theme().keys().spacingDensity(), "default").apply()
                    activity?.applySpacingChanges()
                    activity.hideBinaryRibbon()
                },
                onButton3Click = {
                    prefs.edit().putString(LauncherPreferences.theme().keys().spacingDensity(), "spacious").apply()
                    activity?.applySpacingChanges()
                    activity.hideBinaryRibbon()
                }
            )
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

        // --- APPS ---
        items.add(SettingsItem.Header("hdr_apps", getString(R.string.settings_launcher_section_apps)))
        items.add(SettingsItem.Clickable("btn_hidden", getString(R.string.settings_apps_hidden), null) {
            val intent = Intent(context, AppListActivity::class.java).apply {
                putExtra("hidden", true)
            }
            startActivity(intent)
        })
        items.add(SettingsItem.Toggle("tgl_hide_bound", getString(R.string.settings_apps_hide_bound_apps), null, null, LauncherPreferences.apps().hideBoundApps()) {
            prefs.edit().putBoolean(LauncherPreferences.apps().keys().hideBoundApps(), it).apply()
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

        return items
    }

    /**
     * Refreshes the settings list with a standard submitList() call.
     * Use this for non-font preference changes.
     */
    private fun refreshList() {
        if (!isAdded) return
        adapter.submitList(buildSettingsItems())
    }

    /**
     * Refreshes the settings list and triggers FONT_UPDATE payload after DiffUtil completes.
     * Use this when the font preference changes to avoid race conditions.
     */
    private fun refreshListWithFontUpdate() {
        if (!isAdded) return
        val items = buildSettingsItems()
        adapter.submitList(items) {
            // This callback fires AFTER DiffUtil has completely finished applying the new list
            adapter.notifyItemRangeChanged(0, adapter.itemCount, "FONT_UPDATE")
        }
    }

    /**
     * Refreshes the settings list and triggers SPACING_UPDATE payload after DiffUtil completes.
     * Use this when the spacing preference changes to update text sizes without rebuilding the list.
     */
    internal fun refreshListWithSpacingUpdate() {
        if (!isAdded) return
        adapter.notifyItemRangeChanged(0, adapter.itemCount, "SPACING_UPDATE")
    }
}
