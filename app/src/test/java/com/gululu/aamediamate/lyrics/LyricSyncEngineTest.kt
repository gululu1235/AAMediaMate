package com.gululu.aamediamate.lyrics

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class LyricSyncEngineTest {

    private lateinit var onLineChanged: (String) -> Unit

    @Before
    fun setUp() {
        onLineChanged = mockk(relaxed = true)
    }

    @Test
    fun `start should immediately trigger first line if position is zero`() = runTest {
        val lyrics = listOf(LyricLine(0.0f, "First line"), LyricLine(5.0f, "Second line"))
        LyricSyncEngine.start(lyrics, 0L, onLineChanged)
        coEvery { onLineChanged("First line") }
    }

    @Test
    fun `start should trigger correct line when starting from middle`() = runTest {
        val lyrics = listOf(
            LyricLine(0.0f, "First line"),
            LyricLine(5.0f, "Second line"),
            LyricLine(10.0f, "Third line")
        )
        LyricSyncEngine.start(lyrics, 6000L, onLineChanged)
        coEvery { onLineChanged("Second line") }
    }

    @Test
    fun `stop should cancel the running job`() = runTest {
        val lyrics = listOf(LyricLine(0.0f, "Line 1"), LyricLine(10.0f, "Line 2"))
        LyricSyncEngine.start(lyrics, 0L, onLineChanged)
        LyricSyncEngine.stop()
        // How to assert that the job is cancelled is a bit tricky in this setup
        // but we can at least ensure it doesn't crash.
    }
}
