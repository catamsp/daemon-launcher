package com.catamsp.Daemon.ui.settings.actions

import android.app.Activity
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.catamsp.Daemon.R
import com.catamsp.Daemon.actions.Action
import com.catamsp.Daemon.actions.Gesture
import com.catamsp.Daemon.databinding.SettingsActionsRecyclerBinding
import com.catamsp.Daemon.preferences.LauncherPreferences
import com.catamsp.Daemon.ui.UIObject
import com.catamsp.Daemon.ui.list.SelectActionActivity
import com.catamsp.Daemon.ui.settings.SettingsItem
import com.catamsp.Daemon.ui.settings.SettingsRecyclerAdapter
import kotlinx.coroutines.*

class SettingsFragmentActionsRecycler : Fragment(), UIObject {

    private var savedScrollPosition = 0
    private val adapter = SettingsRecyclerAdapter()

    private var sharedPreferencesListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            refreshList()
        }
    
    private lateinit var binding: SettingsActionsRecyclerBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = SettingsActionsRecyclerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.settingsActionsRview.layoutManager = LinearLayoutManager(context)
        binding.settingsActionsRview.adapter = adapter
        
        refreshList()
    }

    override fun onStart() {
        super<Fragment>.onStart()
        LauncherPreferences.getSharedPreferences()
            .registerOnSharedPreferenceChangeListener(sharedPreferencesListener)
        super<UIObject>.onStart()
    }

    override fun onDestroy() {
        LauncherPreferences.getSharedPreferences()
            .unregisterOnSharedPreferenceChangeListener(sharedPreferencesListener)
        super.onDestroy()
    }

    override fun onPause() {
        savedScrollPosition =
            (binding.settingsActionsRview.layoutManager as LinearLayoutManager)
                .findFirstCompletelyVisibleItemPosition()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        (binding.settingsActionsRview.layoutManager)?.scrollToPosition(savedScrollPosition)
    }

    private fun refreshList() {
        val activity = requireActivity()
        val prefs = LauncherPreferences.getSharedPreferences()
        val gestures = Gesture.entries.filter(Gesture::isEnabled)
        val items = mutableListOf<SettingsItem>()

        // --- GESTURES CONFIGURATION ---
        items.add(SettingsItem.Header("hdr_gest_cfg", getString(R.string.settings_launcher_section_gestures)))
        items.add(SettingsItem.Toggle("tgl_double_swipe", getString(R.string.settings_enabled_gestures_double_swipe), getString(R.string.settings_enabled_gestures_double_swipe_summary), null, LauncherPreferences.enabled_gestures().doubleSwipe()) {
            prefs.edit().putBoolean(LauncherPreferences.enabled_gestures().keys().doubleSwipe(), it).apply()
            refreshList()
        })
        items.add(SettingsItem.Toggle("tgl_edge_swipe", getString(R.string.settings_enabled_gestures_edge_swipe), getString(R.string.settings_enabled_gestures_edge_swipe_summary), null, LauncherPreferences.enabled_gestures().edgeSwipe()) {
            prefs.edit().putBoolean(LauncherPreferences.enabled_gestures().keys().edgeSwipe(), it).apply()
            refreshList()
        })
        items.add(SettingsItem.Slider("sld_edge_width", getString(R.string.settings_enabled_gestures_edge_swipe_edge_width), null, LauncherPreferences.enabled_gestures().edgeSwipeEdgeWidth(), 0, 33) {
            prefs.edit().putInt(LauncherPreferences.enabled_gestures().keys().edgeSwipeEdgeWidth(), it).apply()
            refreshList()
        })
        items.add(SettingsItem.Toggle("tgl_diag_swipe", getString(R.string.settings_enabled_gestures_diagonal_swipe), null, null, LauncherPreferences.enabled_gestures().diagonalSwipe()) {
            prefs.edit().putBoolean(LauncherPreferences.enabled_gestures().keys().diagonalSwipe(), it).apply()
            refreshList()
        })

        // --- SHORTCUT BINDINGS ---
        items.add(SettingsItem.Header("hdr_gestures", "Action Bindings"))

        lifecycleScope.launch {
            val fontSuffix = LauncherPreferences.theme().font().name
            val settingsItems = withContext(Dispatchers.Default) {
                Gesture.entries.map { gesture ->
                    val action = Action.forGesture(gesture)
                    if (action == null) {
                        SettingsItem.Clickable(
                            itemKey = gesture.id + "_" + fontSuffix,
                            title = gesture.getLabel(activity),
                            description = "Tap to bind an app or action",
                            icon = null
                        ) {
                            SelectActionActivity.selectAction(activity, gesture)
                        }
                    } else {
                        // Get icon/label info (could be heavy, so we do it in Default dispatcher)
                        val data = action.getIconAndContentDescription(activity)
                        SettingsItem.Clickable(
                            itemKey = gesture.id + "_" + fontSuffix,
                            title = gesture.getLabel(activity),
                            description = data.second,
                            icon = data.first,
                            onRemove = { 
                                Action.clearActionForGesture(gesture)
                                refreshList()
                            }
                        ) {
                            SelectActionActivity.selectAction(activity, gesture)
                        }
                    }
                }
            }
            items.addAll(settingsItems)
            adapter.submitList(items)
        }
    }
}
