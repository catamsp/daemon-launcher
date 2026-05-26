package com.catamsp.Daemon.ui.settings.launcher

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.SeekBar
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.catamsp.Daemon.R
import com.catamsp.Daemon.databinding.SettingsLauncherBinding
import com.catamsp.Daemon.preferences.LauncherPreferences
import com.catamsp.Daemon.preferences.theme.Font
import com.catamsp.Daemon.ui.UIObject
import com.catamsp.Daemon.ui.settings.SettingsItem
import com.catamsp.Daemon.ui.settings.SettingsRecyclerAdapter
import com.catamsp.Daemon.ui.widgets.manage.ManageWidgetPanelsActivity
import com.catamsp.Daemon.ui.widgets.manage.ManageWidgetsActivity
import com.catamsp.Daemon.widgets.ClockWidget
import com.catamsp.Daemon.widgets.GlobeWidget

class SettingsFragmentWidgets : Fragment(), UIObject {

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
        val activeWidgets = LauncherPreferences.widgets().widgets() ?: emptySet()

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
            
            val fonts = Font.entries
            items.add(SettingsItem.Clickable("btn_clock_font", getString(R.string.settings_clock_font), "Current: ${LauncherPreferences.clock().font().name.lowercase().replaceFirstChar { it.uppercase() }}") {
                showSingleChoiceDialog(getString(R.string.settings_clock_font),
                    fonts.map { it.name.lowercase().replaceFirstChar { it.uppercase() } }.toTypedArray(),
                    fonts.indexOf(LauncherPreferences.clock().font())
                ) { index ->
                    prefs.edit().putString(LauncherPreferences.clock().keys().font(), fonts[index].name).apply()
                    refreshList()
                }
            })

            items.add(SettingsItem.Clickable("btn_clock_color", getString(R.string.settings_clock_color), "Hex: #%08X".format(LauncherPreferences.clock().color())) {
                showColorPickerDialog(LauncherPreferences.clock().color()) { color ->
                    prefs.edit().putInt(LauncherPreferences.clock().keys().color(), color).apply()
                    refreshList()
                }
            })

            items.add(SettingsItem.Toggle("tgl_clock_loc", getString(R.string.settings_clock_localized), null, null, LauncherPreferences.clock().localized()) {
                prefs.edit().putBoolean(LauncherPreferences.clock().keys().localized(), it).apply()
                refreshList()
            })
            items.add(SettingsItem.Toggle("tgl_time", getString(R.string.settings_clock_time_visible), null, null, LauncherPreferences.clock().timeVisible()) {
                prefs.edit().putBoolean(LauncherPreferences.clock().keys().timeVisible(), it).apply()
                refreshList()
            })
            items.add(SettingsItem.Toggle("tgl_seconds", getString(R.string.settings_clock_show_seconds), null, null, LauncherPreferences.clock().showSeconds()) {
                prefs.edit().putBoolean(LauncherPreferences.clock().keys().showSeconds(), it).apply()
                refreshList()
            })
            items.add(SettingsItem.Toggle("tgl_date", getString(R.string.settings_clock_date_visible), null, null, LauncherPreferences.clock().dateVisible()) {
                prefs.edit().putBoolean(LauncherPreferences.clock().keys().dateVisible(), it).apply()
                refreshList()
            })
            items.add(SettingsItem.Toggle("tgl_flip", getString(R.string.settings_clock_flip_date_time), null, null, LauncherPreferences.clock().flipDateTime()) {
                prefs.edit().putBoolean(LauncherPreferences.clock().keys().flipDateTime(), it).apply()
                refreshList()
            })
        }

        // 2. Globe Widget Settings
        if (activeWidgets.any { it is GlobeWidget }) {
            items.add(SettingsItem.Header("hdr_globe", getString(R.string.settings_launcher_section_globe)))
            
            items.add(SettingsItem.Toggle("tgl_globe_persp", getString(R.string.settings_globe_perspective), getString(R.string.settings_globe_perspective_summary), null, LauncherPreferences.globe().perspective()) {
                prefs.edit().putBoolean(LauncherPreferences.globe().keys().perspective(), it).apply()
                refreshList()
            })

            items.add(SettingsItem.Toggle("tgl_globe_glow", getString(R.string.settings_globe_show_glow), null, null, LauncherPreferences.globe().showGlow()) {
                prefs.edit().putBoolean(LauncherPreferences.globe().keys().showGlow(), it).apply()
                refreshList()
            })

            items.add(SettingsItem.Slider("sld_globe_opacity", getString(R.string.settings_globe_glow_opacity), null, LauncherPreferences.globe().glowOpacity(), 0, 255) {
                prefs.edit().putInt(LauncherPreferences.globe().keys().glowOpacity(), it).apply()
                refreshList()
            })
        }

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
