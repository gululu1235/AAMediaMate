package com.gululu.aamediamate.lyrics

import com.gululu.aamediamate.data.LyricsCleanupField
import com.gululu.aamediamate.data.LyricsCleanupRule
import org.junit.Assert.assertEquals
import org.junit.Test

class LyricsCleanupManagerTest {

    @Test
    fun `applyRules cleans artist and title by field`() {
        val rules = listOf(
            LyricsCleanupRule(
                id = "artist",
                name = "Lossless",
                field = LyricsCleanupField.ARTIST,
                pattern = """(?i)\s*[•·-]?\s*Lossless\s*$"""
            ),
            LyricsCleanupRule(
                id = "title",
                name = "Live",
                field = LyricsCleanupField.TITLE,
                pattern = """\s*\(Live\)$"""
            )
        )

        val (title, artist) = LyricsCleanupManager.applyRules(
            title = "Upbeat (Live)",
            artist = "Green Day • Lossless",
            rules = rules
        )

        assertEquals("Upbeat", title)
        assertEquals("Green Day", artist)
    }

    @Test
    fun `applyRules ignores disabled and invalid rules`() {
        val rules = listOf(
            LyricsCleanupRule(
                id = "disabled",
                name = "Disabled",
                field = LyricsCleanupField.ARTIST,
                pattern = """Lossless""",
                isEnabled = false
            ),
            LyricsCleanupRule(
                id = "invalid",
                name = "Invalid",
                field = LyricsCleanupField.ARTIST,
                pattern = """[unclosed"""
            )
        )

        val (title, artist) = LyricsCleanupManager.applyRules(
            title = "Song",
            artist = "Artist Lossless",
            rules = rules
        )

        assertEquals("Song", title)
        assertEquals("Artist Lossless", artist)
    }
}
