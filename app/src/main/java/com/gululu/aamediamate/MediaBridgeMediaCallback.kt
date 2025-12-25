package com.gululu.aamediamate

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import android.view.KeyEvent

class MediaBridgeMediaCallback(private val context: Context) : MediaSessionCompat.Callback() {
    override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
        val keyEvent = mediaButtonEvent?.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)

        if (isSwapEnabled() && keyEvent?.action == KeyEvent.ACTION_DOWN) {
            when (keyEvent.keyCode) {
                KeyEvent.KEYCODE_MEDIA_NEXT -> {
                    Log.d("MediaBridge", "🔘 Intercepted KEYCODE_MEDIA_NEXT -> FastForward")
                    onFastForward()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                    Log.d("MediaBridge", "🔘 Intercepted KEYCODE_MEDIA_PREVIOUS -> Rewind")
                    onRewind()
                    return true
                }
            }
        }
        return super.onMediaButtonEvent(mediaButtonEvent)
    }

    override fun onPlay() {
        Log.d("MediaBridge", "▶️ onPlay triggered")
        MediaControllerManager.getActiveController(context)?.transportControls?.play()
        sync()
    }

    override fun onPlayFromSearch(query: String?, extras: Bundle?) {
        Log.d("MediaBridge", "onPlayFromSearch: query=$query")
        // A simple implementation: just delegate to onPlay() to start playback of the current track
        // or whatever the default play action is. A real implementation would use the query.
        onPlay()
    }

    override fun onPause() {
        Log.d("MediaBridge", "⏸️ onPause triggered")
        MediaControllerManager.getActiveController(context)?.transportControls?.pause()
        sync()
    }

    override fun onSkipToNext() {
        Log.d("MediaBridge", "⏭️ onSkipToNext triggered")
        if (isSwapEnabled()) {
            onFastForward()
        } else {
            MediaControllerManager.getActiveController(context)?.transportControls?.skipToNext()
            sync()
        }
    }

    override fun onSkipToPrevious() {
        Log.d("MediaBridge", "⏮️ onSkipToPrevious triggered")
        if (isSwapEnabled()) {
            onRewind()
        } else {
            MediaControllerManager.getActiveController(context)?.transportControls?.skipToPrevious()
            sync()
        }
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
            MediaStateUpdater.ACTION_SKIP_NEXT -> {
                MediaControllerManager.getActiveController(context)?.transportControls?.skipToNext()
                sync()
            }
            MediaStateUpdater.ACTION_SKIP_PREVIOUS -> {
                MediaControllerManager.getActiveController(context)?.transportControls?.skipToPrevious()
                sync()
            }
            else -> {
                Log.w("MediaBridge", "Unknown custom action: $action")
            }
        }
    }

    private fun isSwapEnabled(): Boolean {
        val controller = MediaControllerManager.getActiveController(context) ?: return false
        return SettingsManager.isAppSwapRewindFastForward(context, controller.packageName)
    }

    private fun sync()
    {
        Handler(Looper.getMainLooper()).postDelayed({
            MediaBridgeSessionManager.updateFromMediaInfo(MediaInformationRetriever.refreshCurrentMediaInfo(context))
        }, 500)
    }
}
