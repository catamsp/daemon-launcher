package com.catamsp.Daemon.ui.tutorial.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.catamsp.Daemon.R
import com.catamsp.Daemon.ui.UIObject

/**
 * The [TutorialFragment2Usage] is a used as a tab in the TutorialActivity.
 *
 * Tells the user how his screen will look and how the app can be used
 */
class TutorialFragment2Usage : Fragment(), UIObject {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.tutorial_2_usage, container, false)
    }

    override fun onStart() {
        super<Fragment>.onStart()
        super<UIObject>.onStart()
    }

}
