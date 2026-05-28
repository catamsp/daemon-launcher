package com.catamsp.Daemon.ui

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import com.catamsp.Daemon.R

class PauseActivity : UIObjectActivity() {

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
                com.catamsp.Daemon.actions.AppAction(appInfo).invoke(this, null, null, ignoreDistracting = true)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            finish()
        }

        object : CountDownTimer(15000, 1000) {
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
}
