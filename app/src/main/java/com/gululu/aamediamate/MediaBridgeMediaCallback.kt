package com.gululu.aamediamate

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.Handler
import android.os.Looper
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log

class MediaBridgeMediaCallback(private val context: Context) : MediaSessionCompat.Callback() {

    private fun getRealController(): MediaController? {
        val targetPackage = MediaBridgeSessionManager.getCurrentMediaPackage() ?: return null
        val sessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val component = ComponentName(context, MediaNotificationListener::class.java)
        val controllers = sessionManager.getActiveSessions(component)
        return controllers.firstOrNull { it.packageName == targetPackage }
    }

    override fun onPlay() {
        Log.d("MediaBridge", "▶️ onPlay triggered")
        getRealController()?.transportControls?.play()
        sync()
    }

    override fun onPause() {
        Log.d("MediaBridge", "⏸️ onPause triggered")
        getRealController()?.transportControls?.pause()
        sync()
    }

    override fun onSkipToNext() {
        Log.d("MediaBridge", "⏭️ onSkipToNext triggered")
        getRealController()?.transportControls?.skipToNext()
        sync()
    }

    override fun onSkipToPrevious() {
        Log.d("MediaBridge", "⏮️ onSkipToPrevious triggered")
        getRealController()?.transportControls?.skipToPrevious()
        sync()
    }

    override fun onSeekTo(pos: Long) {
        Log.d("MediaBridge", "🎯 onSeekTo triggered: $pos ms")
        getRealController()?.transportControls?.seekTo(pos)

        sync()
    }

    override fun onRewind() {
        val controller = getRealController() ?: return
        val pos = controller.playbackState?.position ?: 0L
        val newPos = (pos - 10_000).coerceAtLeast(0L)
        Log.d("MediaBridge", "⏪ Rewind triggered: $newPos ms")
        controller.transportControls.seekTo(newPos)
        sync()
    }

    override fun onFastForward() {
        val controller = getRealController() ?: return
        val pos = controller.playbackState?.position ?: 0L
        val newPos = pos + 10_000
        Log.d("MediaBridge", "⏩ FastForward triggered: $newPos ms")
        controller.transportControls.seekTo(newPos)
        sync()
    }

    private fun sync()
    {
        Handler(Looper.getMainLooper()).postDelayed({
            MediaBridgeSessionManager.updateFromMediaInfo(MediaInformationRetriever.refreshCurrentMediaInfo(context))
        }, 500)
    }
}
