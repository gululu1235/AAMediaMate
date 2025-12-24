package com.gululu.aamediamate.lyrics

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File

class LyricsRepositorySortTest {

    private lateinit var context: Context
    private lateinit var tempDir: File

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        // Use a temporary directory for tests
        tempDir = java.nio.file.Files.createTempDirectory("lyrics_test").toFile()
        every { context.getExternalFilesDir("lyrics") } returns tempDir
        resetLyricCacheDir()
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
        resetLyricCacheDir()
    }

    private fun resetLyricCacheDir() {
        try {
            val field = LyricCache::class.java.getDeclaredField("lyricsDir")
            field.isAccessible = true
            field.set(LyricCache, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Test
    fun `getAllLyrics returns sorted by lastModified descending`() = runTest {
        // Create files
        val file1 = File(tempDir, "SongA_ArtistA.lrt")
        file1.createNewFile()
        
        val file2 = File(tempDir, "SongB_ArtistB.lrt")
        file2.createNewFile()

        // Set timestamps ensuring significant difference
        val now = System.currentTimeMillis()
        file1.setLastModified(now - 10000) // Older
        file2.setLastModified(now)         // Newer

        val result = LyricsRepository.getAllLyrics(context)

        assertEquals("Should find 2 lyrics", 2, result.size)
        assertEquals("First item should be the newer one (SongB)", "SongB", result[0].title)
        assertEquals("Second item should be the older one (SongA)", "SongA", result[1].title)
    }
}
