package com.catamsp.Daemon.ui.settings.launcher

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.catamsp.Daemon.R
import com.catamsp.Daemon.databinding.SettingsLauncherBinding
import com.catamsp.Daemon.preferences.LauncherPreferences
import com.catamsp.Daemon.preferences.theme.TransitionAnimation
import com.catamsp.Daemon.ui.UIObject
import com.catamsp.Daemon.ui.settings.SettingsActivity
import com.catamsp.Daemon.ui.settings.SettingsItem
import com.catamsp.Daemon.ui.settings.SettingsRecyclerAdapter

class SettingsFragmentAnimations : Fragment(), UIObject {

    private lateinit var binding: SettingsLauncherBinding
    private val adapter = SettingsRecyclerAdapter()

    private val sharedPreferencesListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, prefKey ->
            refreshList()
            if (prefKey == LauncherPreferences.theme().keys().font()) {
                view?.post {
                    adapter.notifyItemRangeChanged(0, adapter.itemCount, "FONT_UPDATE")
                }
            }
        }

    private fun getAnimation(prefs: SharedPreferences, key: String): TransitionAnimation {
        val value = prefs.getString(key, TransitionAnimation.FADE.name)
        return try { TransitionAnimation.valueOf(value!!) } catch (e: Exception) { TransitionAnimation.FADE }
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

        // --- MASTER TOGGLE ---
        items.add(SettingsItem.Header("hdr_anim_master", getString(R.string.settings_theme_animations)))
        val masterToggle = prefs.getBoolean(LauncherPreferences.animations().keys().masterToggle(), true)
        items.add(SettingsItem.Toggle("tgl_anim_master", getString(R.string.settings_animations_master_toggle), null, null, masterToggle) {
            prefs.edit().putBoolean(LauncherPreferences.animations().keys().masterToggle(), it).apply()
            refreshList()
        })

        if (masterToggle) {
            val activity = requireActivity() as? SettingsActivity
            val anims = TransitionAnimation.entries.filter { it != TransitionAnimation.NONE }
            val labels = anims.map { it.getLabel(context) }
            
            // Swipe Up
            val currentUp = getAnimation(prefs, LauncherPreferences.animations().keys().swipeUp())
            items.add(SettingsItem.Clickable("btn_anim_up", getString(R.string.settings_animations_swipe_up), "Current: ${currentUp.getLabel(context)}") {
                activity?.showSelectionCarousel("btn_anim_up", anims.indexOf(currentUp), labels) { index: Int ->
                    prefs.edit().putString(LauncherPreferences.animations().keys().swipeUp(), anims[index].name).apply()
                    refreshList()
                }
            })

            // Swipe Down
            val currentDown = getAnimation(prefs, LauncherPreferences.animations().keys().swipeDown())
            items.add(SettingsItem.Clickable("btn_anim_down", getString(R.string.settings_animations_swipe_down), "Current: ${currentDown.getLabel(context)}") {
                activity?.showSelectionCarousel("btn_anim_down", anims.indexOf(currentDown), labels) { index: Int ->
                    prefs.edit().putString(LauncherPreferences.animations().keys().swipeDown(), anims[index].name).apply()
                    refreshList()
                }
            })

            // Swipe Left
            val currentLeft = getAnimation(prefs, LauncherPreferences.animations().keys().swipeLeft())
            items.add(SettingsItem.Clickable("btn_anim_left", getString(R.string.settings_animations_swipe_left), "Current: ${currentLeft.getLabel(context)}") {
                activity?.showSelectionCarousel("btn_anim_left", anims.indexOf(currentLeft), labels) { index: Int ->
                    prefs.edit().putString(LauncherPreferences.animations().keys().swipeLeft(), anims[index].name).apply()
                    refreshList()
                }
            })

            // Swipe Right
            val currentRight = getAnimation(prefs, LauncherPreferences.animations().keys().swipeRight())
            items.add(SettingsItem.Clickable("btn_anim_right", getString(R.string.settings_animations_swipe_right), "Current: ${currentRight.getLabel(context)}") {
                activity?.showSelectionCarousel("btn_anim_right", anims.indexOf(currentRight), labels) { index: Int ->
                    prefs.edit().putString(LauncherPreferences.animations().keys().swipeRight(), anims[index].name).apply()
                    refreshList()
                }
            })

            // Other
            val currentOther = getAnimation(prefs, LauncherPreferences.animations().keys().other())
            items.add(SettingsItem.Clickable("btn_anim_other", getString(R.string.settings_animations_other), "Current: ${currentOther.getLabel(context)}") {
                activity?.showSelectionCarousel("btn_anim_other", anims.indexOf(currentOther), labels) { index: Int ->
                    prefs.edit().putString(LauncherPreferences.animations().keys().other(), anims[index].name).apply()
                    refreshList()
                }
            })
        }

        adapter.submitList(items)
    }
}
