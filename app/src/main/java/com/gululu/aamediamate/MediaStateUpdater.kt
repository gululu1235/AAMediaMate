package com.gululu.aamediamate

import android.content.Context
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.gululu.aamediamate.models.MediaInfo

class MediaStateUpdater(private val context: Context) {

    fun update(mediaSession: MediaSessionCompat, info: MediaInfo) {
        updateMetadata(mediaSession, info)
        updatePlaybackState(mediaSession, info)
        Log.d("MediaBridge", "🎵 Updated MediaSession: ${info.title} by ${info.artist}")
    }

    private fun updateMetadata(mediaSession: MediaSessionCompat, info: MediaInfo) {
        val artist = info.artist.takeIf { it.isNotBlank() }
        val album = info.album.takeIf { it.isNotBlank() }

        val artistAlbum = listOfNotNull(artist, album).joinToString(" - ")

        val metadataBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, info.title)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, info.duration)

        if (artistAlbum.isNotBlank()) {
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artistAlbum)
        }

        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "From ${info.appName}")

        info.albumArt?.let {
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, it)
        }

        mediaSession.setMetadata(metadataBuilder.build())
    }

    private fun updatePlaybackState(mediaSession: MediaSessionCompat, info: MediaInfo) {
        val stateBuilder = PlaybackStateCompat.Builder()
            .setActions(SUPPORTED_ACTIONS)
            .addCustomAction(createRewindAction())
            .addCustomAction(createFastForwardAction())
            .setState(
                if (info.isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                info.position,
                1.0f
            )
        mediaSession.setPlaybackState(stateBuilder.build())
    }

    fun clear(mediaSession: MediaSessionCompat) {
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_NONE, 0, 1.0f)
                .build()
        )
        mediaSession.setMetadata(null)
        Log.d("MediaBridge", "Reset session states.")
    }

    private fun createRewindAction(): PlaybackStateCompat.CustomAction {
        return PlaybackStateCompat.CustomAction.Builder(
            ACTION_REWIND_10S,
            context.getString(R.string.action_rewind_10s),
            android.R.drawable.ic_media_rew
        ).build()
    }

    private fun createFastForwardAction(): PlaybackStateCompat.CustomAction {
        return PlaybackStateCompat.CustomAction.Builder(
            ACTION_FAST_FORWARD_10S,
            context.getString(R.string.action_fast_forward_10s),
            android.R.drawable.ic_media_ff
        ).build()
    }

    companion object {
        const val ACTION_REWIND_10S = "action_rewind_10s"
        const val ACTION_FAST_FORWARD_10S = "action_fast_forward_10s"

        const val SUPPORTED_ACTIONS =
            PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_SEEK_TO or
                    PlaybackStateCompat.ACTION_REWIND or
                    PlaybackStateCompat.ACTION_FAST_FORWARD
    }
}
