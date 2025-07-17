package com.gululu.aamediamate

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log

class MediaBridgeMediaCallback(private val context: Context) : MediaSessionCompat.Callback() {
    override fun onPlay() {
        Log.d("MediaBridge", "▶️ onPlay triggered")
        MediaControllerManager.getActiveController(context)?.transportControls?.play()
        sync()
    }

    override fun onPause() {
        Log.d("MediaBridge", "⏸️ onPause triggered")
        MediaControllerManager.getActiveController(context)?.transportControls?.pause()
        sync()
    }

    override fun onSkipToNext() {
        Log.d("MediaBridge", "⏭️ onSkipToNext triggered")
        MediaControllerManager.getActiveController(context)?.transportControls?.skipToNext()
        sync()
    }

    override fun onSkipToPrevious() {
        Log.d("MediaBridge", "⏮️ onSkipToPrevious triggered")
        MediaControllerManager.getActiveController(context)?.transportControls?.skipToPrevious()
        sync()
    }

    override fun onSeekTo(pos: Long) {
        Log.d("MediaBridge", "🎯 onSeekTo triggered: $pos ms")
        MediaControllerManager.getActiveController(context)?.transportControls?.seekTo(pos)

        sync()
    }

    override fun onRewind() {
        val controller = MediaControllerManager.getActiveController(context) ?: return
        val pos = controller.playbackState?.position ?: 0L
        val newPos = (pos - 10_000).coerceAtLeast(0L)
        Log.d("MediaBridge", "⏪ Rewind triggered: $newPos ms")
        controller.transportControls.seekTo(newPos)
        sync()
    }

    override fun onFastForward() {
        val controller = MediaControllerManager.getActiveController(context) ?: return
        val pos = controller.playbackState?.position ?: 0L
        val newPos = pos + 10_000
        Log.d("MediaBridge", "⏩ FastForward triggered: $newPos ms")
        controller.transportControls.seekTo(newPos)
        sync()
    }

    override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
        Log.d("MediaBridge", "onPlayFromMediaId: $mediaId")

        if (mediaId == null) return

        val controller = MediaControllerManager.getAllControllers(context)
            .firstOrNull { it.packageName == mediaId }

        if (controller != null) {
            val info = MediaInformationRetriever.buildMediaInfoFromController(context, controller)
            if (info != null) {
                MediaBridgeSessionManager.updateFromMediaInfo(info)
            }
        }
    }

    override fun onCustomAction(action: String?, extras: Bundle?) {
        Log.d("MediaBridge", "🎯 Custom action triggered: $action")
        
        when (action) {
            MediaStateUpdater.ACTION_REWIND_10S -> {
                onRewind()
            }
            MediaStateUpdater.ACTION_FAST_FORWARD_10S -> {
                onFastForward()
            }
            else -> {
                Log.w("MediaBridge", "Unknown custom action: $action")
            }
        }
    }

    private fun sync()
    {
        Handler(Looper.getMainLooper()).postDelayed({
            MediaBridgeSessionManager.updateFromMediaInfo(MediaInformationRetriever.refreshCurrentMediaInfo(context))
        }, 500)
    }
}
