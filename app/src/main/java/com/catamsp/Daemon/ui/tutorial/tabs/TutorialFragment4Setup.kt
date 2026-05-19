package com.catamsp.Daemon.ui.tutorial.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.catamsp.Daemon.R
import com.catamsp.Daemon.ui.UIObject

/**
 * The [TutorialFragment4Setup] is a used as a tab in the TutorialActivity.
 *
 * It is used to display info in the tutorial
 */
class TutorialFragment4Setup : Fragment(), UIObject {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.tutorial_4_setup, container, false)
    }

    override fun onStart() {
        super<Fragment>.onStart()
        super<UIObject>.onStart()
    }

}
