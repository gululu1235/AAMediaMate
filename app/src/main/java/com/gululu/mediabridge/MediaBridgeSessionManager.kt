package com.gululu.mediabridge

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat

object MediaBridgeSessionManager {
    private var mediaSession: MediaSessionCompat? = null

    fun init(context: Context) {
        if (mediaSession == null) {
            mediaSession = MediaSessionCompat(context, "MediaBridgeSession").apply {
                setCallback(MediaBridgeCallback(context))
                setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
                isActive = true
            }
            setPlaybackState(PlaybackStateCompat.STATE_PLAYING)
            LogBuffer.append("✅ MediaSession initialized and active")
        }
    }

    fun updateMetadata(title: String?, artist: String?, albumArt: Bitmap? = null) {
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title ?: "")
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist ?: "")
            .apply {
                if (albumArt != null) {
                    putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
                }
            }
            .build()

        mediaSession?.setMetadata(metadata)

        Log.d("MediaBridge", "🎵 元数据已更新: $title - $artist")
        LogBuffer.append("🎵 元数据更新: $title - $artist")
        if (albumArt != null) {
            LogBuffer.append("🖼️ 封面图已设置")
        }
    }


    fun setPlaybackState(state: Int) {
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE
            )
            .setState(state, 0, 1.0f)
            .build()

        mediaSession?.setPlaybackState(playbackState)

        Log.d("MediaBridge", "⏯️ 播放状态更新: $state")
    }

    fun getSessionToken(): MediaSessionCompat.Token? = mediaSession?.sessionToken

    fun getSession(): MediaSessionCompat? = mediaSession
}
