package com.gululu.aamediamate.models

data class BridgedApp(
    val packageName: String,
    val appName: String,
    val firstSeen: Long = System.currentTimeMillis(),
    val lastSeen: Long = System.currentTimeMillis(),
    val lyricsEnabled: Boolean = true
)