package com.gululu.mediabridge.lyrics

object LyricCache {
    private val memoryCache = mutableMapOf<String, List<LyricLine>>()

    suspend fun getOrFetchLyrics(title: String, artist: String, duration: String): List<LyricLine> {
        val key = "$title|$artist"
        memoryCache[key]?.let { return it }

        val result = LyricsManager.getLyricsLrc(title, artist, duration)
        if (result.isNotEmpty()) {
            memoryCache[key] = result
        }
        return result
    }
}