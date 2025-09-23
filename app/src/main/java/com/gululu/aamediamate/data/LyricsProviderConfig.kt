package com.gululu.aamediamate.data

import android.content.Context
import com.gululu.aamediamate.R
import com.gululu.aamediamate.lyrics.providers.LrcApiProvider
import com.gululu.aamediamate.lyrics.providers.LRCLibProvider
import com.gululu.aamediamate.lyrics.providers.LyricsProvider
import com.gululu.aamediamate.lyrics.providers.MusixmatchProvider

data class LyricsProviderConfig(
    val id: String,
    val name: String,
    val descriptionRes: Int,
    val isEnabled: Boolean,
    val priority: Int,
    val provider: LyricsProvider
) {
    fun getDescription(context: Context): String = context.getString(descriptionRes)
}

object LyricsProviderRegistry {
    fun getAllProviders(): List<LyricsProviderConfig> = listOf(
        LyricsProviderConfig(
            id = "lrclib",
            name = "LRCLib",
            descriptionRes = R.string.lrclib_description,
            isEnabled = true,
            priority = 1,
            provider = LRCLibProvider
        ),
        LyricsProviderConfig(
            id = "musixmatch",
            name = "Musixmatch",
            descriptionRes = R.string.musixmatch_description,
            isEnabled = true,
            priority = 2,
            provider = MusixmatchProvider
        ),
        LyricsProviderConfig(
            id = "lrc_api",
            name = "LRC API",
            descriptionRes = R.string.lrc_api_description,
            isEnabled = true,
            priority = 3,
            provider = LrcApiProvider
        )
    )
    
    fun getProviderById(id: String): LyricsProviderConfig? {
        return getAllProviders().find { it.id == id }
    }
}