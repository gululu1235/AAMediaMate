package com.gululu.aamediamate

import android.content.Context
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import com.gululu.aamediamate.models.MediaInfo

object MediaBridgeSessionManager {
    private var mediaSession: MediaSessionCompat? = null
    private var mediaStateUpdater: MediaStateUpdater? = null
    private var lyricDisplayManager: LyricDisplayManager? = null
    private var currentMediaInfo: MediaInfo? = null
    private var mediaInfoListener: ((MediaInfo?) -> Unit)? = null

    fun init(context: Context) {
        if (mediaSession != null) return

        val appContext = context.applicationContext
        mediaStateUpdater = MediaStateUpdater(appContext)
        lyricDisplayManager = LyricDisplayManager(appContext)

        mediaSession = MediaSessionCompat(context, "MediaBridgeSession").apply {
            setCallback(MediaBridgeMediaCallback(context))
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            isActive = true
        }

        mediaStateUpdater?.clear(mediaSession!!)
        Log.d("MediaBridge", "✅ MediaSession initialized.")
    }

    fun updateFromMediaInfo(info: MediaInfo?) {
        currentMediaInfo = info
        val session = mediaSession ?: return

        if (info == null) {
            mediaStateUpdater?.clear(session)
            lyricDisplayManager?.stop()
            mediaInfoListener?.invoke(null)
            return
        }

        // Restore original metadata before showing lyrics
        mediaStateUpdater?.update(session, info)
        lyricDisplayManager?.start(session, info)

        mediaInfoListener?.invoke(info)
        MediaBridgeService.refreshBrowserData()
    }

    fun getSessionToken(): MediaSessionCompat.Token? = mediaSession?.sessionToken

    fun getCurrentMediaPackage(): String? = currentMediaInfo?.appPackageName

    fun setMediaInfoListener(listener: (MediaInfo?) -> Unit) {
        mediaInfoListener = listener
    }

    fun clearMediaInfoListener() {
        mediaInfoListener = null
    }

    fun getRewindActionId(): String = MediaStateUpdater.ACTION_REWIND_10S
    fun getFastForwardActionId(): String = MediaStateUpdater.ACTION_FAST_FORWARD_10S
}
