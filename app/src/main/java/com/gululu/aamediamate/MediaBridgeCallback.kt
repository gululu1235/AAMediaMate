package com.gululu.aamediamate

import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.support.v4.media.session.MediaSessionCompat

class MediaBridgeCallback(private val context: Context) : MediaSessionCompat.Callback() {

    private val realController: MediaController? by lazy {
        val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val activeSessions = manager.getActiveSessions(null)
        activeSessions.firstOrNull { it.packageName != context.packageName }
    }

    override fun onPlay() {
        LogBuffer.append("▶️ 用户点击了播放")
        realController?.transportControls?.play()
    }

    override fun onPause() {
        LogBuffer.append("⏸️ 用户点击了暂停")
        realController?.transportControls?.pause()
    }

    override fun onSkipToNext() {
        LogBuffer.append("⏭️ 用户点击了下一首")
        realController?.transportControls?.skipToNext()
    }

    override fun onSkipToPrevious() {
        LogBuffer.append("⏮️ 用户点击了上一首")
        realController?.transportControls?.skipToPrevious()
    }
}
