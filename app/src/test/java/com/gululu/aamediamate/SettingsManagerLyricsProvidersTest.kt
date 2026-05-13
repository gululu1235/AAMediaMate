package com.gululu.aamediamate

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class SettingsManagerLyricsProvidersTest {

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
    fun `default lyrics provider priorities are contiguous`() {
        val providers = SettingsManager.getLyricsProviders(context)

        assertEquals(listOf(1, 2, 3), providers.map { it.priority })
    }

    @Test
    fun `moveProviderPriority moves provider by visible order when saved priorities have gaps`() {
        SettingsManager.saveLyricsProviders(
            context,
            SettingsManager.getLyricsProviders(context).map { provider ->
                if (provider.id == "lrc_api") {
                    provider.copy(priority = 4)
                } else {
                    provider
                }
            }
        )

        SettingsManager.moveProviderPriority(context, "lrc_api", -1)

        val providers = SettingsManager.getLyricsProviders(context)
        assertEquals(listOf("lrclib", "lrc_api", "Spotify"), providers.map { it.id })
        assertEquals(listOf(1, 2, 3), providers.map { it.priority })
    }

    @Test
    fun `moveProviderPriority recovers when saved priorities contain duplicates`() {
        SettingsManager.saveLyricsProviders(
            context,
            SettingsManager.getLyricsProviders(context).map { provider ->
                when (provider.id) {
                    "Spotify" -> provider.copy(priority = 2)
                    "lrc_api" -> provider.copy(priority = 2)
                    else -> provider
                }
            }
        )

        SettingsManager.moveProviderPriority(context, "lrc_api", -1)

        val providers = SettingsManager.getLyricsProviders(context)
        assertEquals(listOf("lrclib", "lrc_api", "Spotify"), providers.map { it.id })
        assertEquals(listOf(1, 2, 3), providers.map { it.priority })
    }

    @Test
    fun `saveLyricsProviders does not persist duplicate priorities`() {
        SettingsManager.saveLyricsProviders(
            context,
            SettingsManager.getLyricsProviders(context).map { provider ->
                if (provider.id == "lrc_api") provider.copy(priority = 2) else provider
            }
        )

        assertEquals(listOf(1, 2, 3), SettingsManager.getLyricsProviders(context).map { it.priority })
    }
}
