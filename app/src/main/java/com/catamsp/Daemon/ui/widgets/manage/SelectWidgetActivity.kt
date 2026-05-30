package com.catamsp.Daemon.ui.widgets.manage

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import java.util.Locale
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.catamsp.Daemon.Application
import com.catamsp.Daemon.R
import com.catamsp.Daemon.databinding.ActivitySelectWidgetBinding
import com.catamsp.Daemon.ui.UIObjectActivity
import com.catamsp.Daemon.widgets.ClockWidget
import com.catamsp.Daemon.widgets.GlobeWidget
import com.catamsp.Daemon.widgets.LauncherAppWidgetProvider
import com.catamsp.Daemon.widgets.LauncherAppIconWidgetProvider
import com.catamsp.Daemon.widgets.LauncherClockWidgetProvider
import com.catamsp.Daemon.widgets.LauncherFolderWidgetProvider
import com.catamsp.Daemon.widgets.LauncherGlobeWidgetProvider
import com.catamsp.Daemon.widgets.LauncherWidgetProvider
import com.catamsp.Daemon.widgets.WidgetPanel
import com.catamsp.Daemon.widgets.WidgetPosition
import com.catamsp.Daemon.widgets.bindAppWidgetOrRequestPermission
import com.catamsp.Daemon.widgets.generateInternalId
import com.catamsp.Daemon.widgets.getAppWidgetProviders
import com.catamsp.Daemon.widgets.updateWidget
import kotlinx.coroutines.*


private const val REQUEST_WIDGET_PERMISSION = 29
private const val REQUEST_PICK_APP_FOR_ICON = 30
private const val REQUEST_PICK_APP_FOR_FOLDER = 31

sealed class WidgetListItem {
    data class Header(val appName: String, val packageName: String, val isExpanded: Boolean) : WidgetListItem()
    data class WidgetItem(val widget: LauncherWidgetProvider) : WidgetListItem()
}

/**
 *  This activity lets the user pick an app widget to add.
 *  It provides an interface similar to [android.appwidget.AppWidgetManager.ACTION_APPWIDGET_PICK],
 *  but shows more information and also shows widgets from other user profiles.
 */
class SelectWidgetActivity : UIObjectActivity() {
    lateinit var binding: ActivitySelectWidgetBinding
    var widgetPanelId: Int = WidgetPanel.HOME.id

    private var isGroupedView = true
    private var searchQuery = ""
    private val expandedGroups = mutableSetOf<String>()
    private lateinit var allWidgets: List<LauncherWidgetProvider>
    private lateinit var viewAdapter: SelectWidgetListAdapter

    private var searchJob: Job? = null
    private val searchScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private fun getAppNameFromPackage(packageName: String): String {
        return try {
            val pm = packageManager
            val ai = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            pm.getApplicationLabel(ai).toString()
        } catch (e: Exception) {
            packageName.split(".").last().replaceFirstChar { it.uppercase() }
        }
    }

    private fun tryBindWidget(info: LauncherWidgetProvider) {
        when (info) {
            is LauncherAppWidgetProvider -> {
                val widgetId =
                    (applicationContext as Application).appWidgetHost.allocateAppWidgetId()
                if (bindAppWidgetOrRequestPermission(
                        this,
                        info.info,
                        widgetId,
                        REQUEST_WIDGET_PERMISSION
                    )
                ) {
                    setResult(
                        RESULT_OK,
                        Intent().also {
                            it.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                            it.putExtra(EXTRA_PANEL_ID, widgetPanelId)
                        }
                    )
                    finish()
                }
            }

            is LauncherClockWidgetProvider -> {
                updateWidget(
                    ClockWidget(
                        generateInternalId(),
                        WidgetPosition(0, 4, 12, 3),
                        widgetPanelId
                    )
                )
                finish()
            }

            is LauncherGlobeWidgetProvider -> {
                updateWidget(
                    GlobeWidget(
                        generateInternalId(),
                        WidgetPosition(0, 7, 12, 5),
                        widgetPanelId
                    )
                )
                finish()
            }

            is LauncherAppIconWidgetProvider -> {
                // CRITICAL: Prevent activity from finishing when app picker opens
                ignoreAutoClose = true
                // Launch app picker
                val intent = Intent(this, com.catamsp.Daemon.ui.list.AppListActivity::class.java).apply {
                    putExtra(com.catamsp.Daemon.ui.list.AbstractListActivity.KEY_FAVORITES_VISIBILITY, com.catamsp.Daemon.apps.AppFilter.Companion.AppSetVisibility.VISIBLE.name)
                    putExtra(com.catamsp.Daemon.ui.list.AbstractListActivity.KEY_HIDDEN_VISIBILITY, com.catamsp.Daemon.apps.AppFilter.Companion.AppSetVisibility.HIDDEN.name)
                    putExtra("pick_mode", true)
                    putExtra("widget_panel_id", widgetPanelId)
                }
                startActivityForResult(intent, REQUEST_PICK_APP_FOR_ICON)
            }

            is LauncherFolderWidgetProvider -> {
                // CRITICAL: Prevent activity from finishing when app picker opens
                ignoreAutoClose = true
                // Launch app picker for folder
                val intent = Intent(this, com.catamsp.Daemon.ui.list.AppListActivity::class.java).apply {
                    putExtra(com.catamsp.Daemon.ui.list.AbstractListActivity.KEY_FAVORITES_VISIBILITY, com.catamsp.Daemon.apps.AppFilter.Companion.AppSetVisibility.VISIBLE.name)
                    putExtra(com.catamsp.Daemon.ui.list.AbstractListActivity.KEY_HIDDEN_VISIBILITY, com.catamsp.Daemon.apps.AppFilter.Companion.AppSetVisibility.HIDDEN.name)
                    putExtra("pick_mode", true)
                    putExtra("pick_folder", true)
                    putExtra("widget_panel_id", widgetPanelId)
                }
                startActivityForResult(intent, REQUEST_PICK_APP_FOR_FOLDER)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySelectWidgetBinding.inflate(layoutInflater)
        setContentView(binding.root)


        widgetPanelId = intent.getIntExtra(EXTRA_PANEL_ID, WidgetPanel.HOME.id)

        allWidgets = getAppWidgetProviders(this)
        viewAdapter = SelectWidgetListAdapter()

        binding.selectWidgetRecycler.apply {
            setHasFixedSize(false)
            setItemViewCacheSize(10)
            layoutManager = LinearLayoutManager(this@SelectWidgetActivity)
            adapter = viewAdapter
        }

        binding.selectWidgetSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim().lowercase(Locale.ROOT)
                if (query == searchQuery) return
                searchQuery = query
                
                searchJob?.cancel()
                searchJob = searchScope.launch {
                    delay(150) // Debounce
                    refreshList()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.selectWidgetGroupToggle.setOnCheckedChangeListener { _, isChecked ->
            isGroupedView = isChecked
            refreshList()
        }

        binding.selectWidgetClose.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

        applyFont(binding.selectWidgetHeading)
        applyFont(binding.selectWidgetSearch)
        applyFont(binding.selectWidgetToolbar)

        refreshList()
    }

    override fun onDestroy() {
        searchScope.cancel()
        super.onDestroy()
    }

    private fun refreshList() {
        searchJob?.cancel()
        searchScope.launch {
            val displayList = withContext(Dispatchers.Default) {
                val list = mutableListOf<WidgetListItem>()

                // 1. Filter widgets by query (Matches App Name OR Widget Name)
                val filteredWidgets = allWidgets.filter { widget ->
                    val pkgName = (widget as? LauncherAppWidgetProvider)?.info?.provider?.packageName ?: "com.catamsp.Daemon"
                    val appName = if (pkgName == "com.catamsp.Daemon") "Daemon Launcher" else getAppNameFromPackage(pkgName)
                    val widgetName = widget.label?.toString() ?: ""
                    
                    searchQuery.isEmpty() || 
                        appName.lowercase(Locale.ROOT).contains(searchQuery) || 
                        widgetName.lowercase(Locale.ROOT).contains(searchQuery)
                }

                // 2. Build the display list
                if (isGroupedView) {
                    val grouped = filteredWidgets.groupBy { (it as? LauncherAppWidgetProvider)?.info?.provider?.packageName ?: "com.catamsp.Daemon" }
                    for ((pkg, widgets) in grouped) {
                        val appName = if (pkg == "com.catamsp.Daemon") "Daemon Launcher" else getAppNameFromPackage(pkg)
                        
                        // CRITICAL: Auto-expand if searching. Otherwise use manual toggle state.
                        val isExpanded = searchQuery.isNotEmpty() || expandedGroups.contains(pkg)

                        list.add(WidgetListItem.Header(appName, pkg, isExpanded))

                        if (isExpanded) {
                            list.addAll(widgets.map { WidgetListItem.WidgetItem(it) })
                        }
                    }
                } else {
                    list.addAll(filteredWidgets.map { WidgetListItem.WidgetItem(it) })
                }
                list
            }

            // 3. Dispatch to DiffUtil for smooth animations
            viewAdapter.submitList(displayList)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_WIDGET_PERMISSION -> {
                if (resultCode == RESULT_OK) {
                    data ?: return
                    val provider =
                        (data.getSerializableExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER) as? AppWidgetProviderInfo)
                            ?: return
                    tryBindWidget(LauncherAppWidgetProvider(provider, this))
                }
            }
            REQUEST_PICK_APP_FOR_ICON -> {
                if (resultCode == RESULT_OK) {
                    data ?: return
                    val appInfoJson = data.getStringExtra("app_info") ?: return
                    val appInfo = try {
                        com.catamsp.Daemon.apps.AppInfo.serializer().let {
                            kotlinx.serialization.json.Json.decodeFromString(it, appInfoJson)
                        }
                    } catch (_: Exception) { return }

                    val widget = com.catamsp.Daemon.widgets.AppIconWidget(
                        appInfo = appInfo,
                        position = com.catamsp.Daemon.widgets.WidgetPosition.findFreeSpace(
                            WidgetPanel.byId(widgetPanelId), 6, 6
                        ),
                        panelId = widgetPanelId,
                        id = generateInternalId()
                    )
                    updateWidget(widget)
                    finish()
                }
            }
            REQUEST_PICK_APP_FOR_FOLDER -> {
                if (resultCode == RESULT_OK) {
                    data ?: return
                    val appInfosJson = data.getStringArrayListExtra("app_infos") ?: return
                    val appInfos = appInfosJson.mapNotNull { json ->
                        try {
                            kotlinx.serialization.json.Json.decodeFromString<com.catamsp.Daemon.apps.AppInfo>(
                                com.catamsp.Daemon.apps.AppInfo.serializer().descriptor.serialName.let { _ ->
                                    json
                                }
                            )
                        } catch (_: Exception) { null }
                    }
                    if (appInfos.isEmpty()) return

                    val widget = com.catamsp.Daemon.widgets.AppFolderWidget(
                        apps = appInfos.toMutableList(),
                        label = "Folder",
                        position = com.catamsp.Daemon.widgets.WidgetPosition.findFreeSpace(
                            WidgetPanel.byId(widgetPanelId), 6, 6
                        ),
                        panelId = widgetPanelId,
                        id = generateInternalId()
                    )
                    updateWidget(widget)
                    finish()
                }
            }
        }
    }

    inner class SelectWidgetListAdapter : ListAdapter<WidgetListItem, RecyclerView.ViewHolder>(WidgetDiffCallback()) {

        private val TYPE_HEADER = 0
        private val TYPE_WIDGET = 1

        override fun getItemViewType(position: Int): Int {
            return when (getItem(position)) {
                is WidgetListItem.Header -> TYPE_HEADER
                is WidgetListItem.WidgetItem -> TYPE_WIDGET
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return if (viewType == TYPE_HEADER) {
                HeaderViewHolder(inflater.inflate(R.layout.list_widgets_header, parent, false))
            } else {
                WidgetViewHolder(inflater.inflate(R.layout.list_widgets_row, parent, false))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            applyFont(holder.itemView)
            when (val item = getItem(position)) {
                is WidgetListItem.Header -> {
                    val hHolder = holder as HeaderViewHolder
                    hHolder.title.text = item.appName
                    hHolder.icon.rotation = if (item.isExpanded) 90f else 0f
                    hHolder.itemView.setOnClickListener {
                        if (searchQuery.isNotEmpty()) return@setOnClickListener // Disable manual folding while searching
                        if (expandedGroups.contains(item.packageName)) expandedGroups.remove(item.packageName)
                        else expandedGroups.add(item.packageName)
                        refreshList()
                    }
                }
                is WidgetListItem.WidgetItem -> {
                    val wHolder = holder as WidgetViewHolder
                    val provider = item.widget
                    wHolder.textView.text = provider.label
                    wHolder.descriptionView.text = provider.description
                    wHolder.descriptionView.visibility = if (provider.description?.isEmpty() == false) View.VISIBLE else View.GONE
                    wHolder.iconView.setImageDrawable(provider.icon)
                    wHolder.previewView.setImageDrawable(provider.previewImage)
                    wHolder.previewView.visibility = if (provider.previewImage != null) View.VISIBLE else View.GONE
                    wHolder.itemView.setOnClickListener { tryBindWidget(provider) }
                }
            }
        }

        inner class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.header_app_name)
            val icon: ImageView = view.findViewById(R.id.header_expand_icon)
        }

        inner class WidgetViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val textView: TextView = itemView.findViewById(R.id.list_widgets_row_name)
            val descriptionView: TextView = itemView.findViewById(R.id.list_widgets_row_description)
            val iconView: ImageView = itemView.findViewById(R.id.list_widgets_row_icon)
            val previewView: ImageView = itemView.findViewById(R.id.list_widgets_row_preview)
        }
    }

    class WidgetDiffCallback : DiffUtil.ItemCallback<WidgetListItem>() {
        override fun areItemsTheSame(oldItem: WidgetListItem, newItem: WidgetListItem): Boolean {
            if (oldItem is WidgetListItem.Header && newItem is WidgetListItem.Header) return oldItem.packageName == newItem.packageName
            if (oldItem is WidgetListItem.WidgetItem && newItem is WidgetListItem.WidgetItem) return oldItem.widget.label == newItem.widget.label
            return false
        }
        override fun areContentsTheSame(oldItem: WidgetListItem, newItem: WidgetListItem): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
    }
}
