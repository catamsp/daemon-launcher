package com.catamsp.Daemon.ui

import android.app.Activity
import android.app.ActivityOptions
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import com.catamsp.Daemon.R
import com.catamsp.Daemon.preferences.LauncherPreferences
import com.catamsp.Daemon.preferences.theme.TransitionAnimation

class PauseActivity : UIObjectActivity() {

    private var countdownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pause)

        applyFont(findViewById(R.id.pause_container))

        val appInfoStr = intent.getStringExtra("app_info")

        val buttonContinue = findViewById<Button>(R.id.button_continue)
        val buttonGoBack = findViewById<Button>(R.id.button_go_back)

        buttonGoBack.setOnClickListener {
            finish()
        }

        buttonContinue.setOnClickListener {
            if (appInfoStr != null) {
                try {
                    val appInfo = com.catamsp.Daemon.apps.AbstractAppInfo.deserialize(appInfoStr) as com.catamsp.Daemon.apps.AppInfo
                    var opts: Bundle? = null
                    if (LauncherPreferences.animations().masterToggle()) {
                        try {
                            val animPref = LauncherPreferences.animations().other()
                            val animIn = animPref.animIn
                            val animOut = animPref.animOut
                            if (animIn != 0 || animOut != 0) {
                                opts = ActivityOptions.makeCustomAnimation(this, animIn, animOut).toBundle()
                            }
                        } catch (_: Exception) {}
                    }
                    com.catamsp.Daemon.actions.AppAction(appInfo).invoke(this, null, opts, ignoreDistracting = true)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            finish()
        }

        countdownTimer = object : CountDownTimer(15000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000) + 1
                buttonContinue.text = "Wait ${seconds}s..."
            }

            override fun onFinish() {
                buttonContinue.text = "Continue to App"
                buttonContinue.isEnabled = true
            }
        }.start()
    }

    override fun onDestroy() {
        countdownTimer?.cancel()
        super.onDestroy()
    }
}
