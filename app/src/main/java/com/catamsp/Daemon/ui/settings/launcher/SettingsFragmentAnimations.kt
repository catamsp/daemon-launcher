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

        // --- MASTER TOGGLE ---
        items.add(SettingsItem.Header("hdr_anim_master", getString(R.string.settings_theme_animations)))
        items.add(SettingsItem.Toggle("tgl_anim_master", getString(R.string.settings_animations_master_toggle), null, null, LauncherPreferences.animations().masterToggle()) {
            prefs.edit().putBoolean(LauncherPreferences.animations().keys().masterToggle(), it).apply()
        })

        if (LauncherPreferences.animations().masterToggle()) {
            val activity = requireActivity() as? SettingsActivity
            
            // Swipe Up
            val currentUp = try { LauncherPreferences.animations().swipeUp() } catch (e: Exception) { TransitionAnimation.FADE }
            items.add(SettingsItem.Clickable("btn_anim_up", getString(R.string.settings_animations_swipe_up), "Current: ${currentUp.getLabel(context)}") {
                activity?.showAnimationCarousel(currentUp) { selected: TransitionAnimation ->
                    prefs.edit().putString(LauncherPreferences.animations().keys().swipeUp(), selected.name).apply()
                }
            })

            // Swipe Down
            val currentDown = try { LauncherPreferences.animations().swipeDown() } catch (e: Exception) { TransitionAnimation.FADE }
            items.add(SettingsItem.Clickable("btn_anim_down", getString(R.string.settings_animations_swipe_down), "Current: ${currentDown.getLabel(context)}") {
                activity?.showAnimationCarousel(currentDown) { selected: TransitionAnimation ->
                    prefs.edit().putString(LauncherPreferences.animations().keys().swipeDown(), selected.name).apply()
                }
            })

            // Swipe Left
            val currentLeft = try { LauncherPreferences.animations().swipeLeft() } catch (e: Exception) { TransitionAnimation.FADE }
            items.add(SettingsItem.Clickable("btn_anim_left", getString(R.string.settings_animations_swipe_left), "Current: ${currentLeft.getLabel(context)}") {
                activity?.showAnimationCarousel(currentLeft) { selected: TransitionAnimation ->
                    prefs.edit().putString(LauncherPreferences.animations().keys().swipeLeft(), selected.name).apply()
                }
            })

            // Swipe Right
            val currentRight = try { LauncherPreferences.animations().swipeRight() } catch (e: Exception) { TransitionAnimation.FADE }
            items.add(SettingsItem.Clickable("btn_anim_right", getString(R.string.settings_animations_swipe_right), "Current: ${currentRight.getLabel(context)}") {
                activity?.showAnimationCarousel(currentRight) { selected: TransitionAnimation ->
                    prefs.edit().putString(LauncherPreferences.animations().keys().swipeRight(), selected.name).apply()
                }
            })

            // Other
            val currentOther = try { LauncherPreferences.animations().other() } catch (e: Exception) { TransitionAnimation.FADE }
            items.add(SettingsItem.Clickable("btn_anim_other", getString(R.string.settings_animations_other), "Current: ${currentOther.getLabel(context)}") {
                activity?.showAnimationCarousel(currentOther) { selected: TransitionAnimation ->
                    prefs.edit().putString(LauncherPreferences.animations().keys().other(), selected.name).apply()
                }
            })
        }

        adapter.submitList(items)
    }
}
