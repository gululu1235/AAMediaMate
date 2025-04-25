package com.gululu.mediabridge

import android.content.Context
import android.util.Log
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.gululu.mediabridge.models.MediaInfo

object MediaBridgeSessionManager {
    private var mediaSession: MediaSessionCompat? = null
    private var appContext: Context? = null
    private var currentMediaInfo: MediaInfo? = null

    fun init(context: Context) {
        if (mediaSession != null) return

        appContext = context.applicationContext

        mediaSession = MediaSessionCompat(context, "MediaBridgeSession").apply {
            setCallback(MediaBridgeMediaCallback(context)) // ✅ 支持播放/暂停/seek 等
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            isActive = true
        }

        // 设置初始播放状态为暂停
        updatePlaybackState(PlaybackStateCompat.STATE_PAUSED, 0L)
        Log.d("MediaBridge", "✅ MediaSession 初始化完成")
    }

    fun getCurrentMediaPackage(): String? = currentMediaInfo?.appName

    fun updateFromMediaInfo(info: MediaInfo?) {
        currentMediaInfo = info

        if (info == null)
        {
            clearSessionMetadata()
            return
        }

        val label = getAppLabel(info.appName)
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, info.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, info.artist + "-" + info.album)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "From $label")
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, info.duration)
            .apply {
                if (info.albumArt != null) {
                    putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, info.albumArt)
                }
            }
            .build()

        val state = PlaybackStateCompat.Builder()
            .setState(
                if (info.isPlaying) PlaybackStateCompat.STATE_PLAYING
                else PlaybackStateCompat.STATE_PAUSED,
                info.position,
                1.0f
            )
            .setActions(SUPPORTED_ACTIONS)
            .build()

        mediaSession?.setMetadata(metadata)
        mediaSession?.setPlaybackState(state)

        Log.d("MediaBridge", "🎵 更新 MediaSession: ${info.title} by ${info.artist}")
    }

    fun clearSessionMetadata() {
        mediaSession?.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_NONE, 0, 1.0f)
                .build()
        )
        mediaSession?.setMetadata(null)
        currentMediaInfo = null
        Log.d("MediaBridge", "🧹 清空桥接状态")
    }

    fun getSessionToken(): MediaSessionCompat.Token? = mediaSession?.sessionToken

    private fun updatePlaybackState(state: Int, position: Long = 0L) {
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(SUPPORTED_ACTIONS)
            .setState(state, position, 1.0f)
            .build()

        mediaSession?.setPlaybackState(playbackState)
        Log.d("MediaBridge", "⏯️ 播放状态更新: $state at $position ms")
    }

    private fun getAppLabel(packageName: String): String {
        return try {
            val pm = appContext!!.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    private const val SUPPORTED_ACTIONS =
        PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_SEEK_TO
}
