package com.catamsp.Daemon.ui.settings.actions

import android.annotation.SuppressLint
import android.app.Activity
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.catamsp.Daemon.Application
import com.catamsp.Daemon.R
import com.catamsp.Daemon.actions.Action
import com.catamsp.Daemon.actions.Gesture
import com.catamsp.Daemon.databinding.SettingsActionsRecyclerBinding
import com.catamsp.Daemon.preferences.LauncherPreferences
import com.catamsp.Daemon.ui.UIObject
import com.catamsp.Daemon.ui.list.SelectActionActivity
import com.catamsp.Daemon.ui.transformMonochrome
import kotlinx.coroutines.*

/**
 *  The [SettingsFragmentActionsRecycler] is a fragment containing the [ActionsRecyclerAdapter],
 *  which displays all selected actions / apps.
 *
 *  It is used in the Tutorial and in Settings
 */
class SettingsFragmentActionsRecycler : Fragment(), UIObject {

    private var savedScrollPosition = 0

    private var sharedPreferencesListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            actionViewAdapter?.updateActions()
        }
    private lateinit var binding: SettingsActionsRecyclerBinding
    private var actionViewAdapter: ActionsRecyclerAdapter? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = SettingsActionsRecyclerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super<Fragment>.onStart()

        // set up the list / recycler
        val actionViewManager = LinearLayoutManager(context)
        actionViewAdapter = ActionsRecyclerAdapter(requireActivity(), this)

        binding.settingsActionsRview.apply {
            // improve performance (since content changes don't change the layout size)
            setHasFixedSize(false) // Fix: item heights vary based on descriptions
            setItemViewCacheSize(10)
            layoutManager = actionViewManager
            adapter = actionViewAdapter

        }
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
}

class ActionsRecyclerAdapter(val activity: Activity, private val fragment: SettingsFragmentActionsRecycler) :
    RecyclerView.Adapter<ActionsRecyclerAdapter.ViewHolder>() {

    private val colorTheme = LauncherPreferences.theme().colorTheme()
    private val monochromeIcons = LauncherPreferences.theme().monochromeIcons()

    private var gesturesList: List<Gesture> = Gesture.entries.filter(Gesture::isEnabled)
    private val dataCache = mutableMapOf<String, Pair<android.graphics.drawable.Drawable?, String>>()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var textView: TextView = itemView.findViewById(R.id.settings_actions_row_name)
        var descriptionTextView: TextView =
            itemView.findViewById(R.id.settings_actions_row_description)
        var img: ImageView = itemView.findViewById(R.id.settings_actions_row_icon_img)
        var chooseButton: Button = itemView.findViewById(R.id.settings_actions_row_button_choose)
        var removeAction: ImageView = itemView.findViewById(R.id.settings_actions_row_remove)
        var iconContainer: View = itemView.findViewById(R.id.settings_actions_row_icon_container)
    }

    private fun updateViewHolder(gesture: Gesture, viewHolder: ViewHolder) {
        val action = Action.forGesture(gesture)

        if (action == null) {
            viewHolder.iconContainer.visibility = View.GONE
            viewHolder.chooseButton.visibility = View.VISIBLE
            return
        }

        viewHolder.iconContainer.visibility = View.VISIBLE
        viewHolder.chooseButton.visibility = View.GONE

        // Check cache first
        val cached = dataCache[gesture.id]
        if (cached != null) {
            viewHolder.img.setImageDrawable(cached.first)
            viewHolder.img.contentDescription = cached.second
        } else {
            // Load asynchronously to avoid UI lag
            viewHolder.img.setImageDrawable(null)
            fragment.lifecycleScope.launch {
                val data = withContext(Dispatchers.Default) {
                    action.getIconAndContentDescription(activity)
                }
                dataCache[gesture.id] = data
                viewHolder.img.setImageDrawable(data.first)
                viewHolder.img.contentDescription = data.second
            }
        }
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        val gesture = gesturesList[i]
        viewHolder.textView.text = gesture.getLabel(activity)
        viewHolder.descriptionTextView.text = gesture.getDescription(activity)

        viewHolder.img.transformMonochrome(monochromeIcons, colorTheme)

        updateViewHolder(gesture, viewHolder)
        
        viewHolder.itemView.setOnClickListener { SelectActionActivity.selectAction(activity, gesture) }
        viewHolder.removeAction.setOnClickListener { 
            Action.clearActionForGesture(gesture)
            // Local update for immediate feedback
            updateActions()
        }
    }

    override fun getItemCount(): Int {
        return gesturesList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view: View = inflater.inflate(R.layout.settings_actions_row, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateActions() {
        dataCache.clear()
        this.gesturesList = Gesture.entries.filter(Gesture::isEnabled)
        notifyDataSetChanged()
    }
}
