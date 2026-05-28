package com.catamsp.Daemon.ui.list.apps

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.request.CachePolicy
import com.catamsp.Daemon.Application
import com.catamsp.Daemon.R
import com.catamsp.Daemon.actions.Action
import com.catamsp.Daemon.actions.Gesture
import com.catamsp.Daemon.apps.AbstractDetailedAppInfo
import com.catamsp.Daemon.apps.AppFilter
import com.catamsp.Daemon.apps.AppInfo
import com.catamsp.Daemon.apps.DetailedAppInfo
import com.catamsp.Daemon.preferences.LauncherPreferences
import com.catamsp.Daemon.preferences.list.AppNameFormat
import com.catamsp.Daemon.preferences.list.ListLayout
import com.catamsp.Daemon.ui.list.AbstractListActivity
import com.catamsp.Daemon.ui.transformMonochrome
import kotlinx.coroutines.*

/**
 * A [RecyclerView] (efficient scrollable list) containing all apps on the users device.
 * The apps details are represented by [AppInfo].
 *
 * @param activity - the activity this is in
 * @param intention - why the list is displayed ("view", "pick")
 * @param forGesture - the action which an app is chosen for (when the intention is "pick")
 */
@SuppressLint("NotifyDataSetChanged")
class AppsRecyclerAdapter(
    val activity: Activity,
    val root: View,
    private val intention: AbstractListActivity.Companion.Intention = AbstractListActivity.Companion.Intention.VIEW,
    private val forGesture: String? = "",
    private var appFilter: AppFilter = AppFilter(activity, ""),
    private val layout: ListLayout,
    private val nameFormat: AppNameFormat
) :
    ListAdapter<AbstractDetailedAppInfo, AppsRecyclerAdapter.ViewHolder>(DiffCallback) {


    private val apps = (activity.applicationContext as Application).apps
    private val theme = LauncherPreferences.theme()
    private val colorTheme = theme.colorTheme()
    private val grayscale = theme.monochromeIcons()

    // temporarily disable auto launch
    var disableAutoLaunch: Boolean = false
    
    private var searchJob: Job? = null
    private val searchScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        apps.observe(this.activity as AppCompatActivity) {
            updateAppsList()
        }
        // updateAppsList() is removed here as it will be triggered by the observer immediately if apps.value is not null
    }

    object DiffCallback : DiffUtil.ItemCallback<AbstractDetailedAppInfo>() {
        override fun areItemsTheSame(
            oldItem: AbstractDetailedAppInfo,
            newItem: AbstractDetailedAppInfo
        ): Boolean {
            return oldItem.getRawInfo() == newItem.getRawInfo()
        }

        override fun areContentsTheSame(
            oldItem: AbstractDetailedAppInfo,
            newItem: AbstractDetailedAppInfo
        ): Boolean {
            return oldItem.getLabel() == newItem.getLabel() &&
                    oldItem.isPrivate() == newItem.isPrivate()
        }
    }


    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        var textView: TextView = itemView.findViewById(R.id.list_apps_row_name)
        var img: ImageView = itemView.findViewById(R.id.list_apps_row_icon)

        override fun onClick(v: View) {
            val rect = Rect()
            img.getGlobalVisibleRect(rect)
            selectItem(bindingAdapterPosition, rect)
        }

        init {
            itemView.setOnClickListener(this)
        }
    }


    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        val app = getItem(i)
        var appLabel = app.getCustomLabel(activity)

        viewHolder.img.transformMonochrome(grayscale, colorTheme)
        viewHolder.img.load(app) {
            crossfade(true)
            diskCachePolicy(CachePolicy.ENABLED)
            memoryCachePolicy(CachePolicy.ENABLED)
        }

        if (layout.useBadgedText) {
            appLabel = activity.packageManager.getUserBadgedLabel(
                appLabel,
                app.getUser(activity)
            ).toString()
        }
        viewHolder.textView.text = nameFormat.format(appLabel)
        
        // CRITICAL FIX: Manually apply the typeface to the app label
        val fontName = com.catamsp.Daemon.preferences.LauncherPreferences.theme().font()
        viewHolder.textView.typeface = com.catamsp.Daemon.preferences.theme.Font.getTypeface(activity, fontName)

        // decide when to show the options popup menu about
        if (intention == AbstractListActivity.Companion.Intention.VIEW) {
            viewHolder.textView.setOnLongClickListener {
                showOptionsPopup(
                    viewHolder,
                    app
                )
            }
            viewHolder.img.setOnLongClickListener {
                showOptionsPopup(
                    viewHolder,
                    app
                )
            }
            // ensure onClicks are actually caught
            viewHolder.textView.setOnClickListener { viewHolder.onClick(viewHolder.textView) }
            viewHolder.img.setOnClickListener { viewHolder.onClick(viewHolder.img) }
        }
    }

    @Suppress("SameReturnValue")
    private fun showOptionsPopup(
        viewHolder: ViewHolder,
        appInfo: AbstractDetailedAppInfo
    ): Boolean {
        //create the popup menu

        val popup = PopupMenu(activity, viewHolder.img)
        popup.inflate(R.menu.menu_app)

        if (!appInfo.isRemovable()) {
            popup.menu.findItem(R.id.app_menu_delete).isVisible = false
        }

        if (appInfo !is DetailedAppInfo) {
            popup.menu.findItem(R.id.app_menu_info).isVisible = false
        }

        if (LauncherPreferences.apps().hidden()?.contains(appInfo.getRawInfo()) == true) {
            popup.menu.findItem(R.id.app_menu_hidden).setTitle(R.string.list_app_hidden_remove)
        }

        if (LauncherPreferences.apps().distractingApps()?.contains(appInfo.getRawInfo()) == true) {
            popup.menu.findItem(R.id.app_menu_distracting).setTitle(R.string.list_app_distracting_remove)
        }

        if (LauncherPreferences.apps().favorites()?.contains(appInfo.getRawInfo()) == true) {
            popup.menu.findItem(R.id.app_menu_favorite).setTitle(R.string.list_app_favorite_remove)
        }


        popup.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.app_menu_delete -> {
                    appInfo.getRawInfo().uninstall(activity); true
                }

                R.id.app_menu_info -> {
                    (appInfo.getRawInfo() as? AppInfo)?.openSettings(activity); true
                }

                R.id.app_menu_favorite -> {
                    appInfo.getRawInfo().toggleFavorite(); true
                }

                R.id.app_menu_hidden -> {
                    appInfo.getRawInfo().toggleHidden(root); true
                }

                R.id.app_menu_distracting -> {
                    appInfo.getRawInfo().toggleDistracting(); true
                }

                R.id.app_menu_rename -> {
                    appInfo.showRenameDialog(activity); true
                }

                else -> false
            }
        }

        (activity as? com.catamsp.Daemon.ui.UIObjectActivity)?.applyFontToMenu(activity, popup.menu)
        popup.show()
        return true
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view: View = inflater.inflate(layout.layoutResource, parent, false)
        val viewHolder = ViewHolder(view)
        return viewHolder
    }

    fun selectItem(pos: Int, rect: Rect = Rect()) {
        if (pos < 0 || pos >= currentList.size) return
        val appInfo = getItem(pos) ?: return
        when (intention) {
            AbstractListActivity.Companion.Intention.VIEW -> {
                appInfo.getAction().invoke(activity, rect)
            }

            AbstractListActivity.Companion.Intention.PICK -> {
                activity.finish()
                forGesture ?: return
                val gesture = Gesture.byId(forGesture) ?: return
                Action.setActionForGesture(gesture, appInfo.getAction())
            }
        }
    }

    fun updateAppsList(triggerAutoLaunch: Boolean = false) {
        searchJob?.cancel()
        searchJob = searchScope.launch {
            if (triggerAutoLaunch) {
                delay(150) // Debounce rapid typing
            }
            
            val filteredList = withContext(Dispatchers.Default) {
                apps.value?.let { appFilter(it) } ?: listOf()
            }
            
            submitList(filteredList)

            // NEW: Double check the query type
            val currentQuery = appFilter.query
            val isAppSearch = com.catamsp.Daemon.apps.SearchRouter.getSearchType(currentQuery) == com.catamsp.Daemon.apps.SearchRouter.SearchType.APP

            if (triggerAutoLaunch &&
                filteredList.size == 1
                && intention == AbstractListActivity.Companion.Intention.VIEW
                && !disableAutoLaunch
                && isAppSearch // ONLY allow auto-launch if it's a standard app search
                && LauncherPreferences.functionality().searchAutoLaunch()
            ) {
                val app = filteredList[0]
                app.getAction().invoke(activity)

                val inputMethodManager =
                    activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(View(activity).windowToken, 0)
            }
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        searchScope.cancel()
    }

    /**
     * The function [setSearchString] is used to search elements within this [RecyclerView].
     */
    fun setSearchString(search: String) {
        appFilter.query = search
        updateAppsList(true)

    }

    fun setFavoritesVisibility(v: AppFilter.Companion.AppSetVisibility) {
        appFilter.favoritesVisibility = v
        updateAppsList()
    }
}
