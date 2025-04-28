package com.gululu.mediabridge.models

import android.graphics.Bitmap

data class MediaInfo(
    val appPackageName: String,
    val appName: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val position: Long,
    val isPlaying: Boolean,
    val albumArt: Bitmap?
)