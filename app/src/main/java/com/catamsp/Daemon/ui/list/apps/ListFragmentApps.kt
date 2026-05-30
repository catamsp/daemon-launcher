package com.catamsp.Daemon.ui.list.apps

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.catamsp.Daemon.R
import com.catamsp.Daemon.apps.AppFilter
import com.catamsp.Daemon.databinding.ListAppsBinding
import com.catamsp.Daemon.preferences.LauncherPreferences
import com.catamsp.Daemon.ui.UIObject
import com.catamsp.Daemon.ui.closeSoftKeyboard
import com.catamsp.Daemon.ui.list.AbstractListActivity
import com.catamsp.Daemon.ui.list.AppListActivity
import com.catamsp.Daemon.ui.openSoftKeyboard
import kotlin.math.absoluteValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


/**
 * The [ListFragmentApps] is used as a tab in ListActivity.
 *
 * It is a list of all installed applications that can be launched.
 */
class ListFragmentApps : Fragment(), UIObject {
    private lateinit var binding: ListAppsBinding
    private lateinit var appsRecyclerAdapter: AppsRecyclerAdapter
    private lateinit var universalResultAdapter: UniversalResultAdapter
    private var searchJob: Job? = null


    private var sharedPreferencesListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            appsRecyclerAdapter.updateAppsList()
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ListAppsBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyFontToSearchView(binding.listAppsSearchview)
    }

    fun refreshAppsList() {
        if (::appsRecyclerAdapter.isInitialized) {
            appsRecyclerAdapter.updateAppsList()
        }
    }

    override fun onStart() {
        super<Fragment>.onStart()
        super<UIObject>.onStart()
        LauncherPreferences.getSharedPreferences()
            .registerOnSharedPreferenceChangeListener(sharedPreferencesListener)

        binding.listAppsCheckBoxFavorites.isChecked =
            ((activity as? AppListActivity)?.favoritesVisibility == AppFilter.Companion.AppSetVisibility.EXCLUSIVE)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        binding.listAppsRview.layoutManager?.let {
            LauncherPreferences.list().layout().updateLayoutManager(requireContext(), it)
        }

    }

    override fun onStop() {
        super.onStop()
        LauncherPreferences.getSharedPreferences()
            .unregisterOnSharedPreferenceChangeListener(sharedPreferencesListener)
    }


    override fun setOnClicks() {}

    override fun adjustLayout() {
        val listActivity = (activity as? AbstractListActivity) ?: return

        appsRecyclerAdapter =
            AppsRecyclerAdapter(
                listActivity, binding.root, listActivity.intention, listActivity.forGesture,
                appFilter = AppFilter(
                    requireContext(),
                    "",
                    favoritesVisibility = listActivity.favoritesVisibility,
                    privateSpaceVisibility = listActivity.privateSpaceVisibility,
                    hiddenVisibility = listActivity.hiddenVisibility
                ),
                layout = LauncherPreferences.list().layout(),
                nameFormat = LauncherPreferences.list().appNameFormat()
            )


        // set up the list / recycler
        binding.listAppsRview.apply {
            setHasFixedSize(false) // Fix: item heights vary based on app name length
            setItemViewCacheSize(10)
            layoutManager = LauncherPreferences.list().layout().layoutManager(context)
                .also {
                    if (LauncherPreferences.list().reverseLayout()) {
                        (it as? LinearLayoutManager)?.reverseLayout = true
                        (it as? GridLayoutManager)?.reverseLayout = true
                    }
                }
            adapter = appsRecyclerAdapter
            if (LauncherPreferences.functionality().searchAutoCloseKeyboard()) {
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    var totalDy: Int = 0
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        totalDy += dy

                        if (totalDy.absoluteValue > 100) {
                            totalDy = 0
                            closeSoftKeyboard(requireActivity())
                        }
                    }
                })
            }
        }

        universalResultAdapter = UniversalResultAdapter()
        binding.listUniversalResults.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = universalResultAdapter
        }

        binding.listAppsSearchview.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(query: String): Boolean {
                val searchType = com.catamsp.Daemon.apps.SearchRouter.getSearchType(query)
                if (searchType != com.catamsp.Daemon.apps.SearchRouter.SearchType.APP) {
                    if (universalResultAdapter.currentList.isNotEmpty()) {
                        universalResultAdapter.currentList[0].action.invoke()
                    }
                    return true
                }

                appsRecyclerAdapter.setSearchString(query)

                if (LauncherPreferences.functionality().searchWeb()) {
                    val i = Intent(Intent.ACTION_WEB_SEARCH).putExtra("query", query)
                    try {
                        activity?.startActivity(i)
                    } catch (_: ActivityNotFoundException) {
                        Toast.makeText(
                            requireContext(),
                            R.string.toast_activity_not_found_search_web,
                            Toast.LENGTH_LONG
                        ).show()
                    }

                } else {
                    appsRecyclerAdapter.selectItem(0)
                }
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {

                if (newText == " " &&
                    !appsRecyclerAdapter.disableAutoLaunch &&
                    (activity as? AbstractListActivity)?.intention
                    == AbstractListActivity.Companion.Intention.VIEW &&
                    LauncherPreferences.functionality().searchAutoLaunch()
                ) {
                    appsRecyclerAdapter.disableAutoLaunch = true
                    binding.listAppsSearchview.apply {
                        queryHint = context.getString(R.string.list_apps_search_hint_no_auto_launch)
                        setQuery("", false)
                    }
                    return false
                }

                val searchType = com.catamsp.Daemon.apps.SearchRouter.getSearchType(newText)

                if (searchType != com.catamsp.Daemon.apps.SearchRouter.SearchType.APP) {
                    // It is a prefix search. Disable auto-launch.
                    appsRecyclerAdapter.disableAutoLaunch = true

                    val cleanQuery = com.catamsp.Daemon.apps.SearchRouter.getCleanQuery(newText, searchType)

                    binding.listAppsRview.visibility = View.GONE
                    binding.listUniversalResults.visibility = View.VISIBLE

                    searchJob?.cancel()
                    searchJob = CoroutineScope(Dispatchers.IO).launch {
                        val results = when (searchType) {
                            com.catamsp.Daemon.apps.SearchRouter.SearchType.MATH -> com.catamsp.Daemon.apps.UniversalSearchProvider.getMathResults(cleanQuery)
                            com.catamsp.Daemon.apps.SearchRouter.SearchType.CONTACT -> activity?.let { com.catamsp.Daemon.apps.UniversalSearchProvider.getContactsResults(it, cleanQuery) } ?: emptyList()
                            com.catamsp.Daemon.apps.SearchRouter.SearchType.FILE -> activity?.let { com.catamsp.Daemon.apps.UniversalSearchProvider.getFilesResults(it, cleanQuery) } ?: emptyList()
                            com.catamsp.Daemon.apps.SearchRouter.SearchType.SETTING -> context?.let { com.catamsp.Daemon.apps.UniversalSearchProvider.getSettingsResults(it, cleanQuery) } ?: emptyList()
                            else -> emptyList()
                        }
                        
                        withContext(Dispatchers.Main) {
                            universalResultAdapter.submitList(results)
                        }
                    }

                    // Pass the raw text to the app adapter so it clears the list (no apps start with "=")
                    appsRecyclerAdapter.setSearchString(newText)
                    return true
                }

                // Normal App Search
                // Restore auto-launch if it was disabled by a prefix, but do NOT restore if it was a space
                if (appsRecyclerAdapter.disableAutoLaunch && newText.trim().isNotEmpty() && !newText.startsWith(" ")) {
                    appsRecyclerAdapter.disableAutoLaunch = false
                }
                
                binding.listAppsRview.visibility = View.VISIBLE
                binding.listUniversalResults.visibility = View.GONE

                appsRecyclerAdapter.setSearchString(newText)
                return false
            }
        })

        binding.listAppsCheckBoxFavorites.setOnClickListener {
            listActivity.favoritesVisibility =
                if (binding.listAppsCheckBoxFavorites.isChecked) {
                    AppFilter.Companion.AppSetVisibility.EXCLUSIVE
                } else {
                    AppFilter.Companion.AppSetVisibility.VISIBLE
                }
            appsRecyclerAdapter.setFavoritesVisibility(listActivity.favoritesVisibility)
            (activity as? AppListActivity)?.updateTitle()
        }

        if (listActivity.intention == AbstractListActivity.Companion.Intention.VIEW
            && LauncherPreferences.functionality().searchAutoOpenKeyboard()
        ) {
            binding.listAppsSearchview.openSoftKeyboard(requireContext())
        }
    }

}
