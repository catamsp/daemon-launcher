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
        val fontSuffix = LauncherPreferences.theme().font().name

        // --- MASTER TOGGLE ---
        items.add(SettingsItem.Header("hdr_anim_master_$fontSuffix", getString(R.string.settings_theme_animations)))
        items.add(SettingsItem.Toggle("tgl_anim_master_$fontSuffix", getString(R.string.settings_animations_master_toggle), null, null, LauncherPreferences.animations().masterToggle()) {
            prefs.edit().putBoolean(LauncherPreferences.animations().keys().masterToggle(), it).apply()
        })

        if (LauncherPreferences.animations().masterToggle()) {
            val activity = requireActivity() as? SettingsActivity
            val anims = TransitionAnimation.entries.filter { it != TransitionAnimation.NONE }
            val labels = anims.map { it.getLabel(context) }
            
            // Swipe Up
            val currentUp = try { LauncherPreferences.animations().swipeUp() } catch (e: Exception) { TransitionAnimation.FADE }
            items.add(SettingsItem.Clickable("btn_anim_up_$fontSuffix", getString(R.string.settings_animations_swipe_up), "Current: ${currentUp.getLabel(context)}") {
                activity?.showSelectionCarousel("btn_anim_up", anims.indexOf(currentUp), labels) { index: Int ->
                    prefs.edit().putString(LauncherPreferences.animations().keys().swipeUp(), anims[index].name).apply()
                }
            })

            // Swipe Down
            val currentDown = try { LauncherPreferences.animations().swipeDown() } catch (e: Exception) { TransitionAnimation.FADE }
            items.add(SettingsItem.Clickable("btn_anim_down_$fontSuffix", getString(R.string.settings_animations_swipe_down), "Current: ${currentDown.getLabel(context)}") {
                activity?.showSelectionCarousel("btn_anim_down", anims.indexOf(currentDown), labels) { index: Int ->
                    prefs.edit().putString(LauncherPreferences.animations().keys().swipeDown(), anims[index].name).apply()
                }
            })

            // Swipe Left
            val currentLeft = try { LauncherPreferences.animations().swipeLeft() } catch (e: Exception) { TransitionAnimation.FADE }
            items.add(SettingsItem.Clickable("btn_anim_left_$fontSuffix", getString(R.string.settings_animations_swipe_left), "Current: ${currentLeft.getLabel(context)}") {
                activity?.showSelectionCarousel("btn_anim_left", anims.indexOf(currentLeft), labels) { index: Int ->
                    prefs.edit().putString(LauncherPreferences.animations().keys().swipeLeft(), anims[index].name).apply()
                }
            })

            // Swipe Right
            val currentRight = try { LauncherPreferences.animations().swipeRight() } catch (e: Exception) { TransitionAnimation.FADE }
            items.add(SettingsItem.Clickable("btn_anim_right_$fontSuffix", getString(R.string.settings_animations_swipe_right), "Current: ${currentRight.getLabel(context)}") {
                activity?.showSelectionCarousel("btn_anim_right", anims.indexOf(currentRight), labels) { index: Int ->
                    prefs.edit().putString(LauncherPreferences.animations().keys().swipeRight(), anims[index].name).apply()
                }
            })

            // Other
            val currentOther = try { LauncherPreferences.animations().other() } catch (e: Exception) { TransitionAnimation.FADE }
            items.add(SettingsItem.Clickable("btn_anim_other_$fontSuffix", getString(R.string.settings_animations_other), "Current: ${currentOther.getLabel(context)}") {
                activity?.showSelectionCarousel("btn_anim_other", anims.indexOf(currentOther), labels) { index: Int ->
                    prefs.edit().putString(LauncherPreferences.animations().keys().other(), anims[index].name).apply()
                }
            })
        }

        adapter.submitList(items)
    }
}
