package com.gululu.aamediamate

import android.content.Context
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import com.gululu.aamediamate.models.MediaInfo
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.reflect.Method

@RunWith(RobolectricTestRunner::class)
class LyricDisplayManagerTest {

    private val context = mockk<Context>(relaxed = true)
    private val mediaSession = mockk<MediaSessionCompat>(relaxed = true)
    private val manager = LyricDisplayManager(context)

    private fun invokeUpdateLyricLine(info: MediaInfo, lyricLine: String) {
        val method: Method = LyricDisplayManager::class.java.getDeclaredMethod(
            "updateLyricLine",
            MediaSessionCompat::class.java,
            MediaInfo::class.java,
            String::class.java
        )
        method.isAccessible = true
        method.invoke(manager, mediaSession, info, lyricLine)
    }

    @Test
    fun `updateLyricLine with lyrics sets correct metadata`() {
        val mediaInfo = MediaInfo(
            title = "Song Title",
            artist = "Artist Name",
            album = "Album Name",
            appName = "MusicApp",
            appPackageName = "com.music.app",
            duration = 1000L,
            isPlaying = true,
            position = 0L,
            albumArt = null,
            appIcon = null
        )

        invokeUpdateLyricLine(mediaInfo, "Singing lyrics...")

        val slot = slot<MediaMetadataCompat>()
        verify { mediaSession.setMetadata(capture(slot)) }

        val metadata = slot.captured
        
        // Verify Album is always "From [App Name]"
        assertEquals("From MusicApp", metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM))
        
        // Verify Title is the lyric line
        assertEquals("Singing lyrics...", metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE))
        
        // Verify Artist is "Title - Artist - Album"
        assertEquals("Song Title - Artist Name - Album Name", metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST))
    }

    @Test
    fun `updateLyricLine without lyrics sets correct metadata (Restoration)`() {
        val mediaInfo = MediaInfo(
            title = "Song Title",
            artist = "Artist Name",
            album = "Album Name",
            appName = "MusicApp",
            appPackageName = "com.music.app",
            duration = 1000L,
            isPlaying = true,
            position = 0L,
            albumArt = null,
            appIcon = null
        )

        invokeUpdateLyricLine(mediaInfo, "")

        val slot = slot<MediaMetadataCompat>()
        verify { mediaSession.setMetadata(capture(slot)) }

        val metadata = slot.captured

        // Verify Album is always "From [App Name]"
        assertEquals("From MusicApp", metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM))

        // Verify Title is restored to original title
        assertEquals("Song Title", metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE))

        // Verify Artist is restored to "Artist - Album"
        assertEquals("Artist Name - Album Name", metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST))
    }

    @Test
    fun `updateLyricLine without lyrics and empty album sets correct metadata`() {
        val mediaInfo = MediaInfo(
            title = "Song Title",
            artist = "Artist Name",
            album = "", // Empty album
            appName = "MusicApp",
            appPackageName = "com.music.app",
            duration = 1000L,
            isPlaying = true,
            position = 0L,
            albumArt = null,
            appIcon = null
        )

        invokeUpdateLyricLine(mediaInfo, "")

        val slot = slot<MediaMetadataCompat>()
        verify { mediaSession.setMetadata(capture(slot)) }

        val metadata = slot.captured

        // Verify Album is always "From [App Name]"
        assertEquals("From MusicApp", metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM))

        // Verify Title is restored to original title
        assertEquals("Song Title", metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE))

        // Verify Artist is just "Artist Name" (no trailing dash)
        assertEquals("Artist Name", metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST))
    }
}