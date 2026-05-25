package com.catamsp.Daemon.ui.settings.launcher

import android.app.AlertDialog
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
        val items = mutableListOf<SettingsItem>()
        val prefs = LauncherPreferences.getSharedPreferences()
        val context = requireContext()

        // --- MASTER TOGGLE ---
        items.add(SettingsItem.Header("hdr_anim_master", getString(R.string.settings_theme_animations)))
        items.add(SettingsItem.Toggle("tgl_anim_master", getString(R.string.settings_animations_master_toggle), null, null, LauncherPreferences.animations().masterToggle()) {
            prefs.edit().putBoolean(LauncherPreferences.animations().keys().masterToggle(), it).apply()
        })

        if (LauncherPreferences.animations().masterToggle()) {
            // --- DIRECTIONAL ANIMATIONS ---
            items.add(SettingsItem.Header("hdr_anim_directional", getString(R.string.settings_animations_section_directional)))

            val anims = TransitionAnimation.entries
            
            // Swipe Up
            val currentUp = try { LauncherPreferences.animations().swipeUp() } catch (e: Exception) { TransitionAnimation.FADE }
            items.add(SettingsItem.Clickable("btn_anim_up", getString(R.string.settings_animations_swipe_up), "Current: ${currentUp.getLabel(context)}") {
                showSingleChoiceDialog(getString(R.string.settings_animations_swipe_up),
                    anims.map { it.getLabel(context) }.toTypedArray(),
                    anims.indexOf(currentUp)
                ) { index ->
                    prefs.edit().putString(LauncherPreferences.animations().keys().swipeUp(), anims[index].name).apply()
                }
            })

            // Swipe Down
            val currentDown = try { LauncherPreferences.animations().swipeDown() } catch (e: Exception) { TransitionAnimation.FADE }
            items.add(SettingsItem.Clickable("btn_anim_down", getString(R.string.settings_animations_swipe_down), "Current: ${currentDown.getLabel(context)}") {
                showSingleChoiceDialog(getString(R.string.settings_animations_swipe_down),
                    anims.map { it.getLabel(context) }.toTypedArray(),
                    anims.indexOf(currentDown)
                ) { index ->
                    prefs.edit().putString(LauncherPreferences.animations().keys().swipeDown(), anims[index].name).apply()
                }
            })

            // Swipe Left
            val currentLeft = try { LauncherPreferences.animations().swipeLeft() } catch (e: Exception) { TransitionAnimation.FADE }
            items.add(SettingsItem.Clickable("btn_anim_left", getString(R.string.settings_animations_swipe_left), "Current: ${currentLeft.getLabel(context)}") {
                showSingleChoiceDialog(getString(R.string.settings_animations_swipe_left),
                    anims.map { it.getLabel(context) }.toTypedArray(),
                    anims.indexOf(currentLeft)
                ) { index ->
                    prefs.edit().putString(LauncherPreferences.animations().keys().swipeLeft(), anims[index].name).apply()
                }
            })

            // Swipe Right
            val currentRight = try { LauncherPreferences.animations().swipeRight() } catch (e: Exception) { TransitionAnimation.FADE }
            items.add(SettingsItem.Clickable("btn_anim_right", getString(R.string.settings_animations_swipe_right), "Current: ${currentRight.getLabel(context)}") {
                showSingleChoiceDialog(getString(R.string.settings_animations_swipe_right),
                    anims.map { it.getLabel(context) }.toTypedArray(),
                    anims.indexOf(currentRight)
                ) { index ->
                    prefs.edit().putString(LauncherPreferences.animations().keys().swipeRight(), anims[index].name).apply()
                }
            })

            // Other
            val currentOther = try { LauncherPreferences.animations().other() } catch (e: Exception) { TransitionAnimation.FADE }
            items.add(SettingsItem.Clickable("btn_anim_other", getString(R.string.settings_animations_other), "Current: ${currentOther.getLabel(context)}") {
                showSingleChoiceDialog(getString(R.string.settings_animations_other),
                    anims.map { it.getLabel(context) }.toTypedArray(),
                    anims.indexOf(currentOther)
                ) { index ->
                    prefs.edit().putString(LauncherPreferences.animations().keys().other(), anims[index].name).apply()
                }
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
}
