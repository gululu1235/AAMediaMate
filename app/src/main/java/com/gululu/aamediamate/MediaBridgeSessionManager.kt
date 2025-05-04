package com.gululu.aamediamate

import android.content.Context
import android.util.Log
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.gululu.aamediamate.lyrics.LyricCache
import com.gululu.aamediamate.lyrics.LyricSyncEngine
import com.gululu.aamediamate.models.MediaInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object MediaBridgeSessionManager {
    private var mediaSession: MediaSessionCompat? = null
    private var appContext: Context? = null
    private var currentMediaInfo: MediaInfo? = null

    private var mediaInfoListener: ((MediaInfo?) -> Unit)? = null

    fun setMediaInfoListener(listener: (MediaInfo?) -> Unit) {
        mediaInfoListener = listener
    }

    fun clearMediaInfoListener() {
        mediaInfoListener = null
    }

    fun init(context: Context) {
        if (mediaSession != null) return

        appContext = context.applicationContext

        @Suppress("DEPRECATION")
        mediaSession = MediaSessionCompat(context, "MediaBridgeSession").apply {
            setCallback(MediaBridgeMediaCallback(context))
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            isActive = true
        }

        val playbackState = PlaybackStateCompat.Builder()
            .setActions(SUPPORTED_ACTIONS)
            .setState(PlaybackStateCompat.STATE_PAUSED, 0L, 1.0f)
            .build()

        mediaSession?.setPlaybackState(playbackState)
        Log.d("MediaBridge", "✅ MediaSession initialized.")
    }

    fun getCurrentMediaPackage(): String? = currentMediaInfo?.appPackageName

    fun updateFromMediaInfo(info: MediaInfo?) {
        currentMediaInfo = info

        if (info == null)
        {
            clearSessionMetadata()
            mediaInfoListener?.invoke(null)
            return
        }

        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, info.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, info.artist + "-" + info.album)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "From ${info.appName}")
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, info.duration)
            .apply {
                if (info.albumArt != null) {
                    putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, info.albumArt)
                }
            }
            .build()

        val state = PlaybackStateCompat.Builder()
            .setActions(SUPPORTED_ACTIONS)
            .setState(
                if (info.isPlaying) PlaybackStateCompat.STATE_PLAYING
                else PlaybackStateCompat.STATE_PAUSED,
                info.position,
                1.0f
            )
            .build()

        mediaSession?.setMetadata(metadata)
        mediaSession?.setPlaybackState(state)

        Log.d("MediaBridge", "🎵 Updating MediaSession: ${info.title} by ${info.artist}")
        tryStartLyricsSync(info, mediaSession)
        mediaInfoListener?.invoke(info)
    }

    private val lyricsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentLyricsJob: Job? = null
    private val lyricsJobMutex = Mutex()

    private fun tryStartLyricsSync(info: MediaInfo, mediaSession: MediaSessionCompat?) {
        if (appContext == null)
        {
            return
        }

        val enabled = SettingsManager.getLyricsEnabled(appContext!!)
        if (!enabled || !info.isPlaying || info.title.isBlank() || info.artist.isBlank()) {
            LyricSyncEngine.stop()
            return
        }



        lyricsScope.launch {
            lyricsJobMutex.withLock {
                currentLyricsJob?.cancelAndJoin()
                currentLyricsJob = launch {
                    val lyrics = LyricCache.getOrFetchLyrics(
                        appContext!!,
                        info.title,
                        info.artist,
                        info.duration.toString()
                    )
                    if (lyrics.isEmpty()) {
                        Log.d("MediaBridge", "🚫 Lyrics not found: ${info.title}")
                        return@launch
                    }

                    Log.d("MediaBridge", "🎤 Start lyrics sync: ${info.title}")

                    LyricSyncEngine.start(lyrics, info.position) { line ->
                        val metadata = MediaMetadataCompat.Builder()
                            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, line) // Lyrics
                            .putString(
                                MediaMetadataCompat.METADATA_KEY_ARTIST,
                                "${info.title} - ${info.artist} - ${info.album}"
                            )
                            .putString(
                                MediaMetadataCompat.METADATA_KEY_ALBUM,
                                "From ${info.appName}"
                            )
                            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, info.duration)
                            .apply {
                                if (info.albumArt != null) {
                                    putBitmap(
                                        MediaMetadataCompat.METADATA_KEY_ALBUM_ART,
                                        info.albumArt
                                    )
                                }
                            }
                            .build()

                        mediaSession?.setMetadata(metadata)
                    }
                }
            }
        }
    }

    fun getSessionToken(): MediaSessionCompat.Token? = mediaSession?.sessionToken

    private fun clearSessionMetadata() {
        mediaSession?.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_NONE, 0, 1.0f)
                .build()
        )
        mediaSession?.setMetadata(null)
        currentMediaInfo = null
        Log.d("MediaBridge", "Reset session states.")
    }

    private const val SUPPORTED_ACTIONS =
        PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_SEEK_TO or
                PlaybackStateCompat.ACTION_REWIND or
                PlaybackStateCompat.ACTION_FAST_FORWARD
}
