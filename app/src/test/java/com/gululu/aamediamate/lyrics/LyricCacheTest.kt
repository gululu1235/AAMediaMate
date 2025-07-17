package com.gululu.aamediamate.lyrics

import android.content.Context
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class LyricCacheTest {

    private lateinit var context: Context
    private lateinit var lyricsDir: File

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        lyricsDir = File("build/tmp/test_lyrics")
        lyricsDir.mkdirs()
        coEvery { context.getExternalFilesDir("lyrics") } returns lyricsDir
    }

    @Test
    fun `getOrFetchLyrics should return from memory cache if present`() = runTest {
        val title = "Test Title"
        val artist = "Test Artist"
        val key = "${title}_${artist}"
        val cachedLyrics = listOf(LyricLine(0f, "Hello"))
        LyricCache.clearMemoryCache(key) // Ensure cache is clean
        LyricCache.getOrFetchLyrics(context, title, artist, "180") // Prime cache

        val result = LyricCache.getOrFetchLyrics(context, title, artist, "180")

    }

    @Test
    fun `getOrFetchLyrics should return from file cache if not in memory`() = runTest {
        val title = "File Cache Title"
        val artist = "File Cache Artist"
        val key = "${title}_${artist}"
        val lrcContent = "[00:01.00]Test lyric"
        val file = File(lyricsDir, "$key.lrt")
        file.writeText(lrcContent)

        LyricCache.clearMemoryCache(key) // Ensure not in memory

        val result = LyricCache.getOrFetchLyrics(context, title, artist, "180")

    }

    @Test
    fun `getOrFetchLyrics should fetch from network if not in any cache`() {
        val title = "Network Title"
        val artist = "Network Artist"
        val duration = "200"
        val key = "${title}_${artist}"
        val networkLrc = "[00:02.00]From network"
        val parsedLyrics = listOf(LyricLine(2.0f, "From network"))

        io.mockk.mockkObject(LyricsManager)
        coEvery { LyricsManager.getLyricsLrt(context, title, artist, duration) } returns networkLrc
        io.mockk.every { LyricsManager.parseLrc(context, networkLrc) } returns parsedLyrics

        LyricCache.clearMemoryCache(key)
        File(lyricsDir, "$key.lrt").delete()

        val result = kotlinx.coroutines.runBlocking { LyricCache.getOrFetchLyrics(context, title, artist, duration) }

        assert(result == parsedLyrics)
    }
}
