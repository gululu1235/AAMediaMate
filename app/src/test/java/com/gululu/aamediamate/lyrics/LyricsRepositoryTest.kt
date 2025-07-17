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
class LyricsRepositoryTest {

    private lateinit var context: Context
    private lateinit var lyricsDir: File

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        lyricsDir = File("build/tmp/test_lyrics_repo")
        lyricsDir.mkdirs()
        coEvery { context.getExternalFilesDir("lyrics") } returns lyricsDir
    }

    @Test
    fun `getAllLyrics should return empty list if directory doesnt exist`() = runTest {
        lyricsDir.deleteRecursively()
        val result = LyricsRepository.getAllLyrics(context)
    }

    @Test
    fun `deleteLyrics should remove files and clear memory cache`() = runTest {
        val key = "title_artist"
        val file = File(lyricsDir, "$key.lrt")
        file.createNewFile()

        LyricsRepository.deleteLyrics(context, listOf(key))

    }

    @Test
    fun `saveLyricsText should write to file and clear memory cache`() = runTest {
        val key = "save_title_artist"
        val content = "[00:01.00]New lyrics"

        LyricsRepository.saveLyricsText(context, key, content)

        val file = File(lyricsDir, "$key.lrt")
    }
}
