package com.gululu.aamediamate

import android.content.Context
import androidx.core.content.edit
import com.gululu.aamediamate.data.LyricsCleanupField
import com.gululu.aamediamate.data.LyricsCleanupRule
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class SettingsManagerLyricsCleanupTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("media_bridge_settings", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun `getLyricsCleanupRules seeds default Lossless rule on first read`() {
        val rules = SettingsManager.getLyricsCleanupRules(context)

        assertEquals(1, rules.size)
        assertEquals("Lossless", rules.first().name)
        assertEquals(LyricsCleanupField.ARTIST, rules.first().field)
        assertEquals(true, rules.first().isEnabled)
    }

    @Test
    fun `saveLyricsCleanupRules preserves order and values`() {
        val rules = listOf(
            LyricsCleanupRule(
                id = "artist",
                name = "Artist suffix",
                field = LyricsCleanupField.ARTIST,
                pattern = """\s*Suffix$""",
                isEnabled = true
            ),
            LyricsCleanupRule(
                id = "title",
                name = "Title suffix",
                field = LyricsCleanupField.TITLE,
                pattern = """\s*\(Live\)$""",
                isEnabled = false
            )
        )

        SettingsManager.saveLyricsCleanupRules(context, rules)

        val restored = SettingsManager.getLyricsCleanupRules(context)

        assertEquals(rules, restored)
    }
}
