package com.catamsp.Daemon.ui.settings

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.catamsp.Daemon.R
import com.catamsp.Daemon.databinding.SettingsBinding
import com.catamsp.Daemon.preferences.LauncherPreferences
import com.catamsp.Daemon.preferences.theme.Background
import com.catamsp.Daemon.preferences.theme.ColorTheme
import com.catamsp.Daemon.ui.UIObjectActivity
import com.catamsp.Daemon.ui.settings.actions.SettingsFragmentActionsRecycler
import com.catamsp.Daemon.ui.settings.launcher.SettingsFragmentAnimations
import com.catamsp.Daemon.ui.settings.launcher.SettingsFragmentWidgets
import com.catamsp.Daemon.ui.settings.launcher.SettingsFragmentLauncher
import com.catamsp.Daemon.ui.settings.meta.SettingsFragmentMeta
import com.catamsp.Daemon.widgets.ClockWidget

import androidx.core.content.ContextCompat
import com.catamsp.Daemon.preferences.theme.TransitionAnimation

/**
 * The [SettingsActivity] is a tabbed activity:
 *
 * | Actions    |   Choose apps or intents to be launched   | [SettingsFragmentActionsRecycler] |
 * | Theme      |   Select a theme / Customize              | [SettingsFragmentLauncher]   |
 * | Meta       |   About Launcher / Contact etc.           | [SettingsFragmentMeta]    |
 *
 * Settings are closed automatically if the activity goes `onPause` unexpectedly.
 */
class SettingsActivity : UIObjectActivity() {

    private var selectionCarouselListener: ((Int) -> Unit)? = null
    private var currentSelectionItems: List<String> = emptyList()
    private var activeCarouselKey: String? = null

    private val solidBackground = LauncherPreferences.theme().background() == Background.SOLID
            || LauncherPreferences.theme().colorTheme() == ColorTheme.LIGHT

    private val sharedPreferencesListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, prefKey ->
            if (prefKey == LauncherPreferences.theme().keys().font()) {
                val tabAdapter = binding.settingsTabs.adapter as? SettingsTabAdapter
                tabAdapter?.updateFont(LauncherPreferences.theme().font())
                centerTabs()
            }
        }
    private lateinit var binding: SettingsBinding

    private fun centerTabs() {
        binding.settingsTabs.post {
            val currentTab = binding.settingsViewpager.currentItem
            val layoutManager = binding.settingsTabs.layoutManager as? LinearLayoutManager ?: return@post
            
            // Re-calculate padding to ensure centering is possible for any width
            val totalWidth = binding.settingsTabs.width
            val padding = totalWidth / 2
            binding.settingsTabs.setPadding(padding, 0, padding, 0)

            // Attempt to find the view to calculate precise center
            val view = layoutManager.findViewByPosition(currentTab)
            if (view != null) {
                val viewCenter = (view.left + view.right) / 2
                val screenCenter = totalWidth / 2
                binding.settingsTabs.scrollBy(viewCenter - screenCenter, 0)
            } else {
                // Fallback: align start of item to center of screen
                layoutManager.scrollToPositionWithOffset(currentTab, 0)
            }
            
            // Force redrawing of scale/alpha
            binding.settingsTabs.scrollBy(1, 0)
            binding.settingsTabs.scrollBy(-1, 0)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialise layout
        binding = SettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // set up tabs and swiping in settings
        val sectionsPagerAdapter = SettingsSectionsPagerAdapter(this)
        val tabAdapter = SettingsTabAdapter(TAB_TITLES, LauncherPreferences.theme().font()) { position ->
            binding.settingsViewpager.currentItem = position
        }

        binding.settingsViewpager.apply {
            adapter = sectionsPagerAdapter
            setCurrentItem(intent.getIntExtra(EXTRA_TAB, 0), false)
        }

        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.settingsTabs.apply {
            this.layoutManager = layoutManager
            this.adapter = tabAdapter

            // Initial centering
            centerTabs()

            val snapHelper = LinearSnapHelper()
            snapHelper.attachToRecyclerView(this)

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    val centerX = recyclerView.width / 2
                    for (i in 0 until recyclerView.childCount) {
                        val child = recyclerView.getChildAt(i)
                        val childCenterX = (child.left + child.right) / 2
                        val distanceFromCenter = Math.abs(centerX - childCenterX)
                        val scale = 1f - (distanceFromCenter.toFloat() / centerX).coerceIn(0f, 0.4f)
                        child.scaleX = scale
                        child.scaleY = scale
                        child.alpha = 1f - (distanceFromCenter.toFloat() / centerX).coerceIn(0f, 0.6f)
                    }
                }

                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        val centerView = snapHelper.findSnapView(layoutManager)
                        if (centerView != null) {
                            val pos = layoutManager.getPosition(centerView)
                            if (binding.settingsViewpager.currentItem != pos) {
                                binding.settingsViewpager.setCurrentItem(pos, true)
                            }
                        }
                    }
                }
            })
        }

        binding.settingsViewpager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val scroller = object : androidx.recyclerview.widget.LinearSmoothScroller(this@SettingsActivity) {
                    override fun calculateDtToFit(viewStart: Int, viewEnd: Int, boxStart: Int, boxEnd: Int, snapPreference: Int): Int {
                        val childCenter = viewStart + (viewEnd - viewStart) / 2
                        val containerCenter = boxStart + (boxEnd - boxStart) / 2
                        val baseDist = containerCenter - childCenter
                        // Intentionally undershoot by 60px to force the SnapHelper to perform its mechanical "jerk"
                        return if (baseDist > 0) baseDist - 60 else baseDist + 60
                    }
                }
                scroller.targetPosition = position
                layoutManager.startSmoothScroll(scroller)
                hideSelectionCarousel()
            }
        })

        setupAnimationCarousel()
    }

    fun getThemeColor(attrId: Int): Int {
        val typedValue = TypedValue()
        val currentTheme = getTheme() ?: return 0
        currentTheme.resolveAttribute(attrId, typedValue, true)
        return if (typedValue.resourceId != 0) {
            ContextCompat.getColor(this, typedValue.resourceId)
        } else {
            typedValue.data
        }
    }

    private fun setupAnimationCarousel() {
        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(binding.settingsAnimationCarousel)
        
        binding.settingsAnimationCarousel.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                val centerX = rv.width / 2f
                val accentColor = getThemeColor(androidx.appcompat.R.attr.colorAccent)
                val textColor = getThemeColor(android.R.attr.textColor)
                for (i in 0 until rv.childCount) {
                    val child = rv.getChildAt(i)
                    val childCenterX = (child.left + child.right) / 2f
                    val dist = Math.abs(childCenterX - centerX)
                    
                    // Unified scaling and fading logic
                    val scale = 1f - Math.min(dist / centerX * 0.45f, 0.45f)
                    child.scaleX = scale
                    child.scaleY = scale
                    child.alpha = 1f - Math.min(dist / centerX * 0.85f, 0.85f)
                    
                    val text = child.findViewById<TextView>(R.id.animation_name)
                    if (dist < 50f) {
                        text?.setTextColor(accentColor)
                    } else {
                        text?.setTextColor(textColor)
                    }
                }
            }

            override fun onScrollStateChanged(rv: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val centerView = snapHelper.findSnapView(rv.layoutManager)
                    centerView?.let {
                        val pos = rv.layoutManager?.getPosition(it) ?: -1
                        if (pos != RecyclerView.NO_POSITION && pos < currentSelectionItems.size) {
                            selectionCarouselListener?.invoke(pos)
                        }
                    }
                }
            }
        })
    }

    fun showSelectionCarousel(key: String, currentValueIndex: Int, items: List<String>, onSelected: (Int) -> Unit) {
        if (activeCarouselKey == key) {
            hideSelectionCarousel()
            return
        }
        
        activeCarouselKey = key
        selectionCarouselListener = onSelected
        currentSelectionItems = items
        
        val font = LauncherPreferences.theme().font()
        val adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.item_animation_3d, parent, false)
                return object : RecyclerView.ViewHolder(v) {}
            }
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val tv = holder.itemView.findViewById<TextView>(R.id.animation_name)
                tv.text = items[position]
                tv.typeface = font.getTypeface(this@SettingsActivity)
                tv.setTextColor(getThemeColor(android.R.attr.textColor))
                holder.itemView.setOnClickListener {
                    binding.settingsAnimationCarousel.smoothScrollToPosition(position)
                }
            }
            override fun getItemCount(): Int = items.size
        }
        
        binding.settingsAnimationCarousel.apply {
            this.layoutManager = LinearLayoutManager(this@SettingsActivity, LinearLayoutManager.HORIZONTAL, false)
            this.adapter = adapter
            visibility = View.VISIBLE
            
            post {
                val padding = width / 2 - (resources.displayMetrics.density * 45).toInt()
                setPadding(padding, 0, padding, 0)
                if (currentValueIndex != -1) {
                    (layoutManager as LinearLayoutManager).scrollToPositionWithOffset(currentValueIndex, 0)
                }
            }
        }
    }

    fun hideSelectionCarousel() {
        binding.settingsAnimationCarousel.visibility = View.GONE
        selectionCarouselListener = null
        activeCarouselKey = null
    }

    override fun onStart() {
        super.onStart()
        LauncherPreferences.getSharedPreferences()
            .registerOnSharedPreferenceChangeListener(sharedPreferencesListener)
    }

    override fun onPause() {
        LauncherPreferences.getSharedPreferences()
            .unregisterOnSharedPreferenceChangeListener(sharedPreferencesListener)
        super.onPause()
    }

    override fun setOnClicks() {
        // As older APIs somehow do not recognize the xml defined onClick
        binding.settingsClose.setOnClickListener { finish() }
        // open device settings (see https://stackoverflow.com/a/62092663/12787264)
        binding.settingsSystem.setOnClickListener {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    companion object {
        private const val EXTRA_TAB = "tab"
    }
}

private val TAB_TITLES = arrayOf(
    R.string.settings_tab_actions,
    R.string.settings_tab_launcher,
    R.string.settings_tab_animations,
    R.string.settings_tab_widgets,
    R.string.settings_tab_meta
)

class SettingsSectionsPagerAdapter(private val activity: FragmentActivity) :
    FragmentStateAdapter(activity) {

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> SettingsFragmentActionsRecycler()
            1 -> SettingsFragmentLauncher()
            2 -> SettingsFragmentAnimations()
            3 -> SettingsFragmentWidgets()
            4 -> SettingsFragmentMeta()
            else -> Fragment()
        }
    }

    fun getPageTitle(position: Int): CharSequence {
        return activity.resources.getString(TAB_TITLES[position])
    }

    override fun getItemCount(): Int {
        return 5
    }
}

/**
 * A custom adapter for the bottom tab carousel.
 */
class SettingsTabAdapter(
    private val titles: Array<Int>,
    private var font: com.catamsp.Daemon.preferences.theme.Font,
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<SettingsTabAdapter.ViewHolder>() {

    class ViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

    fun updateFont(newFont: com.catamsp.Daemon.preferences.theme.Font) {
        this.font = newFont
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val textView = TextView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, resources.displayMetrics).toInt(),
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, resources.displayMetrics).toInt(),
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, resources.displayMetrics).toInt(),
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, resources.displayMetrics).toInt()
            )
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            gravity = android.view.Gravity.CENTER
            isAllCaps = true
        }
        return ViewHolder(textView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val textColor = (holder.itemView.context as? SettingsActivity)?.getThemeColor(android.R.attr.textColor) 
            ?: holder.itemView.context.getColor(android.R.color.white)
        holder.textView.setText(titles[position])
        holder.textView.typeface = font.getTypeface(holder.itemView.context)
        holder.textView.setTextColor(textColor)
        holder.textView.setOnClickListener { onClick(position) }
    }

    override fun getItemCount(): Int = titles.size
}
