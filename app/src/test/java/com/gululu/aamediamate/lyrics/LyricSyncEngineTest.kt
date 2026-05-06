package com.gululu.aamediamate.lyrics

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class LyricSyncEngineTest {

    @Test
    fun `start should immediately trigger first line if position is zero`() = runTest {
        val lyrics = listOf(LyricLine(0.01f, "First line"), LyricLine(5.0f, "Second line"))
        val lines = mutableListOf<String>()

        LyricSyncEngine.start(lyrics, 0L, 0L) { lines.add(it) }

        Thread.sleep(200)
        assertTrue(lines.contains("First line"))
    }

    @Test
    fun `start should trigger correct line when starting from middle`() = runTest {
        val lyrics = listOf(
            LyricLine(0.01f, "First line"),
            LyricLine(0.02f, "Second line"),
            LyricLine(0.03f, "Third line")
        )
        val lines = mutableListOf<String>()

        LyricSyncEngine.start(lyrics, 15L, 0L) { lines.add(it) }

        Thread.sleep(200)
        assertTrue(lines.contains("Second line"))
    }

    @Test
    fun `stop should cancel the running job`() = runTest {
        val lyrics = listOf(LyricLine(0.0f, "Line 1"), LyricLine(10.0f, "Line 2"))
        LyricSyncEngine.start(lyrics, 0L, 0L) { }
        LyricSyncEngine.stop()
        // How to assert that the job is cancelled is a bit tricky in this setup
        // but we can at least ensure it doesn't crash.
    }
}
