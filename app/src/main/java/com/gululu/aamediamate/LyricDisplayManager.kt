package com.gululu.aamediamate

import android.content.Context
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import com.gululu.aamediamate.lyrics.LyricCache
import com.gululu.aamediamate.lyrics.LyricSyncEngine
import com.gululu.aamediamate.models.MediaInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LyricDisplayManager(private val context: Context) {

    private val lyricsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentLyricsJob: Job? = null
    private val lyricsJobMutex = Mutex()

    fun start(mediaSession: MediaSessionCompat, info: MediaInfo) {
        val enabled = SettingsManager.getLyricsEnabled(context)
        if (!enabled || !info.isPlaying || info.title.isBlank() || info.artist.isBlank()) {
            stop()
            return
        }

        lyricsScope.launch {
            lyricsJobMutex.withLock {
                currentLyricsJob?.cancelAndJoin()
                currentLyricsJob = launch {
                    val lyrics = LyricCache.getOrFetchLyrics(
                        context,
                        info.title,
                        info.artist,
                        info.duration.toString()
                    )

                    if (lyrics.isEmpty()) {
                        Log.d("MediaBridge", "🚫 Lyrics not found: ${info.title}")
                        return@launch
                    }

                    val currentPosition = MediaInformationRetriever.refreshCurrentMediaInfo(context)?.position ?: info.position

                    LyricSyncEngine.start(lyrics, currentPosition) { line ->
                        updateLyricLine(mediaSession, info, line)
                    }
                }
            }
        }
    }

    fun stop() {
        LyricSyncEngine.stop()
        runBlocking {
            lyricsJobMutex.withLock {
                currentLyricsJob?.cancel()
                currentLyricsJob = null
            }
        }
    }

    private fun updateLyricLine(mediaSession: MediaSessionCompat, originalInfo: MediaInfo, lyricLine: String) {
        val artistAlbum = listOfNotNull(originalInfo.artist.takeIf { it.isNotBlank() }, originalInfo.album.takeIf { it.isNotBlank() })
            .joinToString(" - ")

        val metadataBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, lyricLine)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, originalInfo.duration)

        if (artistAlbum.isNotBlank()) {
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "${originalInfo.title} - $artistAlbum")
        }

        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "From ${originalInfo.appName}")

        originalInfo.albumArt?.let {
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, it)
        }

        mediaSession.setMetadata(metadataBuilder.build())
    }
}
