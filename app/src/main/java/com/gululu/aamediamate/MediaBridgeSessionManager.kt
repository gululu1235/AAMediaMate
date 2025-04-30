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
import kotlinx.coroutines.launch

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

        mediaSession = MediaSessionCompat(context, "MediaBridgeSession").apply {
            setCallback(MediaBridgeMediaCallback(context))
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
        tryStartLyricsSync(info, mediaSession)
        mediaInfoListener?.invoke(info)
    }

    private val lyricsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentLyricsJob: Job? = null

    private fun tryStartLyricsSync(info: MediaInfo, mediaSession: MediaSessionCompat?) {
        val enabled = SettingsManager.getLyricsEnabled(appContext!!)
        if (!enabled || !info.isPlaying || info.title.isBlank() || info.artist.isBlank()) {
            LyricSyncEngine.stop()
            return
        }

        currentLyricsJob?.cancel()
        currentLyricsJob = lyricsScope.launch {
            val lyrics = LyricCache.getOrFetchLyrics(appContext!!, info.title, info.artist, info.duration.toString())
            if (lyrics.isEmpty()) {
                Log.d("MediaBridge", "🚫 没有找到歌词: ${info.title}")
                return@launch
            }

            Log.d("MediaBridge", "🎤 开始同步歌词: ${info.title}")

            LyricSyncEngine.start(lyrics, info.position) { line ->
                // 构建同步元数据
                val metadata = MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, line) // 歌词行
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "${info.title} - ${info.artist} - ${info.album}")
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "From ${info.appName}")
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, info.duration)
                    .apply {
                        if (info.albumArt != null) {
                            putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, info.albumArt)
                        }
                    }
                    .build()

                mediaSession?.setMetadata(metadata)
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
        Log.d("MediaBridge", "🧹 清空桥接状态")
    }

    private fun updatePlaybackState(state: Int, position: Long = 0L) {
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(SUPPORTED_ACTIONS)
            .setState(state, position, 1.0f)
            .build()

        mediaSession?.setPlaybackState(playbackState)
        Log.d("MediaBridge", "⏯️ 播放状态更新: $state at $position ms")
    }

    private const val SUPPORTED_ACTIONS =
        PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_SEEK_TO
}
