package com.catamsp.Daemon.ui.settings.launcher

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.catamsp.Daemon.R
import com.catamsp.Daemon.databinding.SettingsLauncherBinding
import com.catamsp.Daemon.preferences.LauncherPreferences
import com.catamsp.Daemon.preferences.theme.Font
import com.catamsp.Daemon.preferences.theme.FontManager
import com.catamsp.Daemon.ui.UIObject
import com.catamsp.Daemon.ui.UIObjectActivity
import com.catamsp.Daemon.ui.settings.ModernColorPickerBottomSheet
import com.catamsp.Daemon.ui.settings.SettingsActivity
import com.catamsp.Daemon.ui.settings.SettingsItem
import com.catamsp.Daemon.ui.settings.SettingsRecyclerAdapter
import com.catamsp.Daemon.ui.widgets.manage.ManageWidgetPanelsActivity
import com.catamsp.Daemon.ui.widgets.manage.ManageWidgetsActivity
import com.catamsp.Daemon.widgets.ClockWidget
import com.catamsp.Daemon.widgets.GlobeWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsFragmentWidgets : Fragment(), UIObject {

    private lateinit var binding: SettingsLauncherBinding
    private val adapter = SettingsRecyclerAdapter()

    private val pickFontLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                val fontName = com.catamsp.Daemon.preferences.theme.FontManager.importFont(requireContext(), it)
                if (fontName != null) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Clock Font imported!", android.widget.Toast.LENGTH_SHORT).show()
                        
                        // SAVE TO CLOCK PREFERENCES
                        val prefs = com.catamsp.Daemon.preferences.LauncherPreferences.getSharedPreferences()
                        prefs.edit().putString(com.catamsp.Daemon.preferences.LauncherPreferences.clock().keys().font(), fontName).apply()
                        
                        refreshList()
                    }
                }
            }
        }
    }

    private val widgetPrefKeys by lazy {
        setOf(
            LauncherPreferences.clock().keys().font(),
            LauncherPreferences.clock().keys().clockSize(),
            LauncherPreferences.clock().keys().color(),
            LauncherPreferences.clock().keys().localized(),
            LauncherPreferences.clock().keys().timeVisible(),
            LauncherPreferences.clock().keys().showSeconds(),
            LauncherPreferences.clock().keys().dateVisible(),
            LauncherPreferences.clock().keys().flipDateTime(),
            LauncherPreferences.globe().keys().perspective(),
            LauncherPreferences.globe().keys().showGlow(),
            LauncherPreferences.globe().keys().glowOpacity(),
            LauncherPreferences.widgets().keys().widgets(),
            LauncherPreferences.theme().keys().font()
        )
    }

    private val sharedPreferencesListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, prefKey ->
            if (isAdded && prefKey in widgetPrefKeys) {
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
        binding.launcherRecyclerView.itemAnimator = null
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
        
        val items = mutableListOf<SettingsItem>()
        val prefs = LauncherPreferences.getSharedPreferences()
        val context = requireContext()
        val activeWidgets = LauncherPreferences.widgets().widgets() ?: emptySet()
        val activity = requireActivity() as? SettingsActivity

        // --- GENERAL WIDGET MANAGEMENT (Always Visible) ---
        items.add(SettingsItem.Header("hdr_widgets", getString(R.string.settings_launcher_section_widgets)))
        items.add(SettingsItem.Clickable("btn_widgets", getString(R.string.settings_widgets_widgets), null) {
            startActivity(Intent(activity, ManageWidgetsActivity::class.java))
        })
        items.add(SettingsItem.Clickable("btn_panels", getString(R.string.settings_widgets_custom_panels), null) {
            startActivity(Intent(activity, ManageWidgetPanelsActivity::class.java))
        })

        // --- SPECIFIC WIDGET CONFIGURATIONS (Dynamic) ---

        // 1. Clock Widget Settings
        if (activeWidgets.any { it is ClockWidget }) {
            items.add(SettingsItem.Header("hdr_clock", getString(R.string.settings_launcher_section_date_time)))
            
            // Build Dynamic Font List (Built-in + Custom)
            val builtInFonts = Font.entries.map { it.name }
            val customFonts = FontManager.getCustomFontNames(context)
            val allFonts = builtInFonts + customFonts
            
            val currentFontName = LauncherPreferences.clock().font()
            val currentFontLabel = try { FontManager.getDisplayName(currentFontName) } catch (e: Exception) { currentFontName }

            items.add(SettingsItem.Clickable(
                "btn_clock_font", 
                getString(R.string.settings_clock_font), 
                "Current: $currentFontLabel",
                onRemove = { 
                    (activity as? UIObjectActivity)?.ignoreAutoClose = true
                    pickFontLauncher.launch(arrayOf("*/*")) 
                }
            ) {
                val displayNames = allFonts.map { FontManager.getDisplayName(it) }
                (activity as? UIObjectActivity)?.ignoreAutoClose = true
                activity?.showSelectionCarousel("btn_clock_font", allFonts.indexOf(currentFontName), displayNames, allFonts) { index: Int ->
                    // CRITICAL: Refresh the lock inside the callback so it survives multiple selections!
                    (activity as? UIObjectActivity)?.ignoreAutoClose = true
                    prefs.edit().putString(LauncherPreferences.clock().keys().font(), allFonts[index]).apply()
                }
            })

            val currentClockSize = LauncherPreferences.clock().clockSize()
            items.add(SettingsItem.Clickable("btn_clock_size", getString(R.string.settings_clock_size), "Current Size: $currentClockSize") {
                (activity as? UIObjectActivity)?.ignoreAutoClose = true
                (activity as? SettingsActivity)?.showSliderCarousel("btn_clock_size", 16, 40, currentClockSize) { newValue ->
                    prefs.edit().putInt(LauncherPreferences.clock().keys().clockSize(), newValue).apply()
                }
            })

            items.add(SettingsItem.Clickable("btn_clock_color", getString(R.string.settings_clock_color), "Hex: #%08X".format(LauncherPreferences.clock().color())) {
                val bottomSheet = ModernColorPickerBottomSheet(LauncherPreferences.clock().color()) { color ->
                    prefs.edit().putInt(LauncherPreferences.clock().keys().color(), color).apply()
                }
                bottomSheet.show(parentFragmentManager, "ColorPicker")
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
        }

        // 2. Globe Widget Settings
        if (activeWidgets.any { it is GlobeWidget }) {
            items.add(SettingsItem.Header("hdr_globe", getString(R.string.settings_launcher_section_globe)))
            
            items.add(SettingsItem.Toggle("tgl_globe_persp", getString(R.string.settings_globe_perspective), getString(R.string.settings_globe_perspective_summary), null, LauncherPreferences.globe().perspective()) {
                prefs.edit().putBoolean(LauncherPreferences.globe().keys().perspective(), it).apply()
            })

            items.add(SettingsItem.Toggle("tgl_globe_glow", getString(R.string.settings_globe_show_glow), null, null, LauncherPreferences.globe().showGlow()) {
                prefs.edit().putBoolean(LauncherPreferences.globe().keys().showGlow(), it).apply()
            })

            val currentGlowOpacity = LauncherPreferences.globe().glowOpacity()
            items.add(SettingsItem.Clickable("btn_globe_glow_opacity", getString(R.string.settings_globe_glow_opacity), "Current Opacity: $currentGlowOpacity") {
                (activity as? UIObjectActivity)?.ignoreAutoClose = true
                (activity as? SettingsActivity)?.showSliderCarousel("btn_globe_glow_opacity", 0, 255, currentGlowOpacity) { newValue ->
                    prefs.edit().putInt(LauncherPreferences.globe().keys().glowOpacity(), newValue).apply()
                }
            })
        }

        val restoreKey = activity?.intent?.getStringExtra("RESTORE_CAROUSEL")
        if (restoreKey != null) {
            activity?.intent?.removeExtra("RESTORE_CAROUSEL")
            binding.root.postDelayed({
                val itemToClick = items.find { it.key == restoreKey } as? SettingsItem.Clickable
                itemToClick?.onClick?.invoke()
            }, 150)
        }

        adapter.submitList(items)
    }
}
