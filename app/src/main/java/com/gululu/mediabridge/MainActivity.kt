package com.gululu.mediabridge

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
}