package com.gululu.mediabridge

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.Observer

class MainActivity : ComponentActivity() {

    private lateinit var logText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        logText = findViewById(R.id.logTextView)

        MediaBridgeSessionManager.init(this)

        LogBuffer.logs.observe(this, Observer { newLog ->
            logText.text = newLog
        })

        LogBuffer.append("✅ MainActivity started")
    }

    private fun launchApp(packageName: String) {
        val pm = packageManager
        val intent = pm.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            LogBuffer.append("✅ 尝试启动 $packageName")
        } else {
            LogBuffer.append("❌ 无法启动 $packageName")
        }
    }
}