package com.catamsp.Daemon.ui.settings.launcher

import android.graphics.Typeface
import androidx.core.content.ContextCompat
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.catamsp.Daemon.R
import com.catamsp.Daemon.preferences.LauncherPreferences
import com.catamsp.Daemon.preferences.theme.TransitionAnimation
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlin.math.*

class AnimationSelectionBottomSheet(
    private val currentAnimation: TransitionAnimation,
    private val onSelected: (TransitionAnimation) -> Unit
) : BottomSheetDialogFragment() {

    private var selectedAnimation: TransitionAnimation = currentAnimation
    private lateinit var currentTypeface: Typeface

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        currentTypeface = LauncherPreferences.theme().font().getTypeface(requireContext())
        return inflater.inflate(R.layout.dialog_animation_selection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.animation_recycler_view)
        val titleView = view.findViewById<TextView>(R.id.animation_dialog_title)

        // Apply dynamic font to title
        titleView.typeface = currentTypeface

        val animations = TransitionAnimation.entries.filter { it != TransitionAnimation.NONE }
        
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        recyclerView.layoutManager = layoutManager
        
        val adapter = AnimationAdapter(animations) { animation ->
            selectedAnimation = animation
            // Auto-save on click as well
            onSelected(animation)
        }
        recyclerView.adapter = adapter

        // Horizontal Carousel Snapping Logic
        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(recyclerView)
        
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val centerView = snapHelper.findSnapView(layoutManager)
                    centerView?.let {
                        val pos = layoutManager.getPosition(it)
                        if (pos != RecyclerView.NO_POSITION) {
                            val newSelection = animations[pos]
                            if (newSelection != selectedAnimation) {
                                selectedAnimation = newSelection
                                // AUTO-SAVE: What is centered is saved
                                onSelected(newSelection)
                                adapter.notifyDataSetChanged()
                            }
                        }
                    }
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val centerX = recyclerView.width / 2f
                for (i in 0 until recyclerView.childCount) {
                    val child = recyclerView.getChildAt(i)
                    val childCenterX = (child.left + child.right) / 2f
                    val distanceFromCenter = abs(childCenterX - centerX)
                    
                    // Simple Minimalist Highlight: Scale and Alpha
                    val scale = 1f - min(distanceFromCenter / centerX * 0.4f, 0.4f)
                    child.scaleX = scale
                    child.scaleY = scale
                    
                    val alpha = 1f - min(distanceFromCenter / centerX, 0.7f)
                    child.alpha = alpha
                }
            }
        })

        recyclerView.post {
            // Add padding so first and last items can be centered horizontally
            val itemWidth = 120f * resources.displayMetrics.density // Estimate
            val padding = (recyclerView.width / 2).toInt() - 60 // Roughly half an item
            recyclerView.setPadding(padding, 0, padding, 0)
            
            val index = animations.indexOf(currentAnimation)
            if (index != -1) {
                layoutManager.scrollToPositionWithOffset(index, 0)
                recyclerView.post {
                    val viewAtPos = layoutManager.findViewByPosition(index)
                    if (viewAtPos != null) {
                        val offset = (recyclerView.width / 2) - (viewAtPos.width / 2)
                        layoutManager.scrollToPositionWithOffset(index, offset)
                    }
                }
            }
        }
    }

    override fun getTheme(): Int = R.style.CustomBottomSheetDialog

    private inner class AnimationAdapter(
        private val items: List<TransitionAnimation>,
        private val onClick: (TransitionAnimation) -> Unit
    ) : RecyclerView.Adapter<AnimationAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.animation_name)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_animation_3d, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.name.text = item.getLabel(requireContext())
            holder.name.typeface = currentTypeface
            
            // Highlight the centered/selected item with accent color
            if (item == selectedAnimation) {
                holder.name.setTextColor(ContextCompat.getColor(requireContext(), R.color.daemonTheme_accent_color))
                holder.name.alpha = 1.0f
            } else {
                holder.name.setTextColor(ContextCompat.getColor(requireContext(), R.color.daemonTheme_text_color))
                holder.name.alpha = 0.4f
            }
            
            holder.itemView.setOnClickListener {
                onClick(item)
                val rv = holder.itemView.parent as RecyclerView
                val offset = (rv.width / 2) - (holder.itemView.width / 2)
                (rv.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, offset)
            }
        }

        override fun getItemCount(): Int = items.size
    }
}