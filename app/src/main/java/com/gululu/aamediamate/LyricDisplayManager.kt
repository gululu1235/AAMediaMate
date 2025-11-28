package com.gululu.aamediamate

import android.content.Context
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import com.gululu.aamediamate.lyrics.LyricCache
import com.gululu.aamediamate.lyrics.LyricSyncEngine
import com.gululu.aamediamate.lyrics.LyricsRepository
import com.gululu.aamediamate.models.MediaInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LyricDisplayManager(private val context: Context) {

    private val lyricsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentLyricsJob: Job? = null
    private val lyricsJobMutex = Mutex()
    private var lyricsUpdateJob: Job? = null
    private var currentMediaInfo: MediaInfo? = null

    fun start(mediaSession: MediaSessionCompat, info: MediaInfo) {
        val globalLyricsEnabled = SettingsManager.getLyricsEnabled(context)
        val appLyricsEnabled = SettingsManager.isAppLyricsEnabled(context, info.appPackageName)
        
        if (!globalLyricsEnabled || !appLyricsEnabled || !info.isPlaying || info.title.isBlank() || info.artist.isBlank()) {
            if (!globalLyricsEnabled) {
                Log.d("MediaBridge", "🚫 Lyrics globally disabled")
            } else if (!appLyricsEnabled) {
                Log.d("MediaBridge", "🚫 Lyrics disabled for app: ${info.appPackageName}")
            }
            stop()
            return
        }
        
        Log.d("MediaBridge", "🎵 Starting lyrics for: ${info.appPackageName} - ${info.title} by ${info.artist}")
        currentMediaInfo = info

        // Start observing lyric updates
        lyricsUpdateJob?.cancel() // Cancel any previous observation
        lyricsUpdateJob = lyricsScope.launch {
            val observedMediaInfo = info // Capture the media info that started this observation
            LyricsRepository.lyricsUpdatedFlow.collectLatest { updatedKey ->
                val currentKey = "${observedMediaInfo.title}_${observedMediaInfo.artist}"
                if (updatedKey == currentKey) {
                    Log.d("MediaBridge", "🎤 Lyrics for current song updated. Restarting lyric display.")
                    // Stop internal components and restart to refresh with new lyrics
                    stopInternal()
                    start(mediaSession, observedMediaInfo)
                }
            }
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
                        updateLyricLine(mediaSession, info, "") // Clear the displayed lyric
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
        stopInternal()
        lyricsUpdateJob?.cancel()
        currentMediaInfo = null
    }

    private fun stopInternal() {
        LyricSyncEngine.stop()
        runBlocking {
            lyricsJobMutex.withLock {
                currentLyricsJob?.cancel()
                currentLyricsJob = null
            }
        }
    }

    private fun updateLyricLine(mediaSession: MediaSessionCompat, originalInfo: MediaInfo, lyricLine: String) {
        val metadataBuilder = MediaMetadataCompat.Builder()
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, originalInfo.duration)

        // Always set the album to "From [App Name]"
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "From ${originalInfo.appName}")

        if (lyricLine.isNotBlank()) {
            // When a lyric is displayed, use the lyric as the title
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, lyricLine)

            // Consolidate song title, artist, and album into the ARTIST field
            val songInfo = listOfNotNull(
                originalInfo.title.takeIf { it.isNotBlank() },
                originalInfo.artist.takeIf { it.isNotBlank() },
                originalInfo.album.takeIf { it.isNotBlank() }
            ).joinToString(" - ")
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, songInfo)

        } else {
            // When no lyric is displayed, restore the original media info formatted correctly
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, originalInfo.title)

            val artist = originalInfo.artist.takeIf { it.isNotBlank() }
            val album = originalInfo.album.takeIf { it.isNotBlank() }
            val artistAlbum = listOfNotNull(artist, album).joinToString(" - ")

            if (artistAlbum.isNotBlank()) {
                metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artistAlbum)
            }
        }

        originalInfo.albumArt?.let {
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, it)
        }

        mediaSession.setMetadata(metadataBuilder.build())
    }
}
