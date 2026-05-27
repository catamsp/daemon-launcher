package com.catamsp.Daemon.ui.settings.meta

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.catamsp.Daemon.BuildConfig
import com.catamsp.Daemon.R
import com.catamsp.Daemon.copyToClipboard
import com.catamsp.Daemon.databinding.SettingsMetaBinding
import com.catamsp.Daemon.getDeviceInfo
import com.catamsp.Daemon.openInBrowser
import com.catamsp.Daemon.preferences.LauncherPreferences
import com.catamsp.Daemon.preferences.resetPreferences
import com.catamsp.Daemon.ui.LegalInfoActivity
import com.catamsp.Daemon.ui.UIObject
import com.catamsp.Daemon.ui.settings.SettingsItem
import com.catamsp.Daemon.ui.settings.SettingsRecyclerAdapter

/**
 * The [SettingsFragmentMeta] is a used as a tab in the SettingsActivity.
 */
class SettingsFragmentMeta : Fragment(), UIObject {

    private lateinit var binding: SettingsMetaBinding
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = SettingsMetaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.metaRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.metaRecyclerView.adapter = adapter
        
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

        items.add(SettingsItem.Header("hdr_help", "Support & Help"))
        
        items.add(SettingsItem.Clickable("btn_reset", getString(R.string.settings_meta_reset), "Wipe all settings and restart") {
            AlertDialog.Builder(context, R.style.AlertDialogCustom)
                .setTitle(getString(R.string.settings_meta_reset))
                .setMessage(getString(R.string.settings_meta_reset_confirm))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    resetPreferences(context)
                    requireActivity().finish()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()
        })

        items.add(SettingsItem.Header("hdr_dev", "Development"))

        items.add(SettingsItem.Clickable("btn_code", getString(R.string.settings_meta_view_code), "View source on GitHub") {
            openInBrowser(getString(R.string.settings_meta_link_github), context)
        })

        items.add(SettingsItem.Clickable("btn_docs", getString(R.string.settings_meta_view_docs), "Read the official documentation") {
            openInBrowser(getString(R.string.settings_meta_link_docs), context)
        })

        items.add(SettingsItem.Clickable("btn_bug", getString(R.string.settings_meta_report_bug), "Report issues or suggest features") {
            showReportBugDialog()
        })

        items.add(SettingsItem.Header("hdr_legal", "Legal"))

        items.add(SettingsItem.Clickable("btn_licenses", getString(R.string.settings_meta_licenses), "Open source credits") {
            startActivity(Intent(context, LegalInfoActivity::class.java))
        })

        items.add(SettingsItem.Clickable("btn_version", "Version: ${BuildConfig.VERSION_NAME}", "Tap to copy debug info") {
            val deviceInfo = getDeviceInfo()
            copyToClipboard(context, deviceInfo)
        })

        adapter.submitList(items)
    }

    private fun showReportBugDialog() {
        val deviceInfo = getDeviceInfo()
        val context = requireContext()
        AlertDialog.Builder(context, R.style.AlertDialogCustom).apply {
            setView(R.layout.dialog_report_bug)
            setTitle(R.string.dialog_report_bug_title)
            setPositiveButton(R.string.dialog_report_bug_create_report) { _, _ ->
                openInBrowser(getString(R.string.settings_meta_report_bug_link), context)
            }
            setNegativeButton(R.string.dialog_cancel) { _, _ -> }
        }.create().also { it.show() }.apply {
            val info = findViewById<TextView>(R.id.dialog_report_bug_device_info)
            val buttonClipboard = findViewById<Button>(R.id.dialog_report_bug_button_clipboard)
            val buttonSecurity = findViewById<Button>(R.id.dialog_report_bug_button_security)
            info?.text = deviceInfo
            buttonClipboard?.setOnClickListener { copyToClipboard(context, deviceInfo) }
            info?.setOnClickListener { copyToClipboard(context, deviceInfo) }
            buttonSecurity?.setOnClickListener {
                openInBrowser(getString(R.string.settings_meta_report_vulnerability_link), context)
            }
        }
    }
}
