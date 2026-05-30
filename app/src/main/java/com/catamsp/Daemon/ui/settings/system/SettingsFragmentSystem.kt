package com.catamsp.Daemon.ui.settings.system

import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.catamsp.Daemon.R
import com.catamsp.Daemon.databinding.SettingsLauncherBinding
import com.catamsp.Daemon.preferences.LauncherPreferences
import com.catamsp.Daemon.ui.settings.SettingsItem
import com.catamsp.Daemon.ui.settings.SettingsRecyclerAdapter
import rikka.shizuku.Shizuku

class SettingsFragmentSystem : Fragment(R.layout.settings_launcher) {

    private var _binding: SettingsLauncherBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: SettingsRecyclerAdapter
    private val items = ArrayList<SettingsItem>()

    // Listener for permission prompt results
    private val requestPermissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == 100) {
            refreshList()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = SettingsLauncherBinding.bind(view)
        
        adapter = SettingsRecyclerAdapter() 
        
        binding.launcherRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.launcherRecyclerView.adapter = adapter
        binding.launcherRecyclerView.itemAnimator = null 

        Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)
        refreshList()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Shizuku.removeRequestPermissionResultListener(requestPermissionResultListener)
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        LauncherPreferences.getSharedPreferences()
            .registerOnSharedPreferenceChangeListener(prefsListener)
        refreshList()
    }

    override fun onPause() {
        LauncherPreferences.getSharedPreferences()
            .unregisterOnSharedPreferenceChangeListener(prefsListener)
        super.onPause()
    }

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, prefKey ->
        if (prefKey == LauncherPreferences.internal().keys().killSpaceEnabled()) {
            refreshList()
        }
    }

    private fun refreshList() {
        items.clear()
        items.add(SettingsItem.Header("hdr_sys", "SYSTEM HOOKS"))

        val shizukuRunning = try { Shizuku.pingBinder() } catch (e: Exception) { false }
        val shizukuGranted = if (shizukuRunning) Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED else false

        val statusText = when {
            !shizukuRunning -> "Not Running (Start via Shizuku app)"
            !shizukuGranted -> "Running - Tap to Grant Permission"
            else -> "Running & Connected"
        }

        items.add(SettingsItem.Clickable("btn_shizuku", "Shizuku Status", statusText) {
            if (shizukuRunning && !shizukuGranted) {
                try {
                    Shizuku.requestPermission(100)
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to request Shizuku permission", Toast.LENGTH_SHORT).show()
                }
            } else if (!shizukuRunning) {
                Toast.makeText(context, "Please start Shizuku first", Toast.LENGTH_SHORT).show()
            }
        })

        items.add(SettingsItem.Header("hdr_eng", "ENGINES"))

        val isKillSpaceEnabled = LauncherPreferences.internal().killSpaceEnabled()
        
        items.add(SettingsItem.Toggle("tgl_kill_space", "Kill Space Engine", "Requires Shizuku Connection", null, isKillSpaceEnabled) { enabled ->
            if (enabled && !shizukuGranted) {
                Toast.makeText(context, "Shizuku permission required", Toast.LENGTH_SHORT).show()
                refreshList() 
            } else {
                LauncherPreferences.internal().killSpaceEnabled(enabled)
            }
        })

        adapter.submitList(items.toList())
    }
}
