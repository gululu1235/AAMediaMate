package com.gululu.aamediamate.lyrics

import android.content.Context
import com.gululu.aamediamate.SettingsManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class LyricsManagerTest {

    private lateinit var mockContext: Context

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        mockkObject(SettingsManager)
        // Mock the Simplified/Traditional conversion setting
        every { SettingsManager.getSimplifyEnabled(any()) } returns true
    }

    @Test
    fun testParseLrc_withMultipleTimestamps() {
        val lrc = """
            [ar:Artist]
            [ti:Title]
            [00:01.50]Line 1
            [00:02.00][00:03.00]Line 2
            [00:04.00]
            Invalid Line
            [00:05.00]Line 3
        """.trimIndent()

        val expected = listOf(
            LyricLine(1.5f, "Line 1"),
            LyricLine(2.0f, "Line 2"),
            LyricLine(3.0f, "Line 2"),
            LyricLine(4.0f, ""),
            LyricLine(5.0f, "Line 3")
        )

        val result = LyricsManager.parseLrc(mockContext, lrc)

        assertEquals(expected.size, result.size)
        for (i in expected.indices) {
            assertEquals(expected[i].timeSec, result[i].timeSec, 0.01f)
            assertEquals(expected[i].text, result[i].text)
        }
    }

    @Test
    fun testParseLrc_emptyInput() {
        val lrc = ""
        val result = LyricsManager.parseLrc(mockContext, lrc)
        assertEquals(0, result.size)
    }

    @Test
    fun testParseLrc_noTimestamps() {
        val lrc = "Just some text without any timestamps"
        val result = LyricsManager.parseLrc(mockContext, lrc)
        assertEquals(0, result.size)
    }

    @Test
    fun testParseLrc_onlyMetadata() {
        val lrc = """
            [ar: Some Artist]
            [ti: Some Title]
            [al: Some Album]
        """.trimIndent()
        val result = LyricsManager.parseLrc(mockContext, lrc)
        assertEquals(0, result.size)
    }
}