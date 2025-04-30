package com.gululu.aamediamate.lyrics

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object LyricCache {
    private val memoryCache = mutableMapOf<String, List<LyricLine>?>()
    private var lyricsDir: File? = null
    fun getLyricsDir(context: Context): File {
        if (lyricsDir == null)
        {
            val dir = context.getExternalFilesDir("lyrics")!!
            if (!dir.exists()) dir.mkdirs()
            lyricsDir = dir
        }

        return lyricsDir!!
    }

    private fun sanitizeFileName(input: String): String {
        return input.replace(Regex("[\\\\/:*?\"<>|]"), "_")
    }

    private fun getLyricFile(context: Context, title: String, artist: String): File {
        return File(getLyricsDir(context), "${sanitizeFileName(title)}_${sanitizeFileName(artist)}.lrt")
    }

    suspend fun getOrFetchLyrics(context: Context, title: String, artist: String, duration: String): List<LyricLine> {
        val key = "$title|$artist"

        // 1. 内存
        memoryCache[key]?.let { cached ->
            Log.d("MediaBridge", "Reading from mem cache")
            return cached ?: emptyList()
        }

        val file = getLyricFile(context, title, artist)

        // 2. 磁盘
        if (file.exists()) {
            val lrcContent = file.readText()
            Log.d("MediaBridge", "Reading from file. $file")

            if (lrcContent.isBlank()) {
                Log.d("MediaBridge", "Reading from file, but no lyrics found.")
                memoryCache[key] = null
                return emptyList()
            }

            val lyrics = LyricsManager.parseLrc(context, lrcContent)
            memoryCache[key] = lyrics
            return lyrics
        }

        Log.d("MediaBridge", "Not cached, reading from API.")
        // 3. 网络
        val lrcContent = LyricsManager.getLyricsLrt(context, title, artist, duration)
        if (!lrcContent.isNullOrBlank()) {
            val lyrics = LyricsManager.parseLrc(context, lrcContent)
            if (lyrics.isNotEmpty()) {
                memoryCache[key] = lyrics
                file.writeText(lrcContent)
                Log.d("MediaBridge", "Saving lyrics to file.")
                return lyrics
            } else {
                memoryCache[key] = null
                withContext(Dispatchers.IO) {
                    file.createNewFile()
                }
                Log.d("MediaBridge", "Failed to get lyrics.")
                return emptyList()
            }
        } else {
            memoryCache[key] = null
            withContext(Dispatchers.IO) {
                file.createNewFile()
            }
            return emptyList()
        }
    }

    fun clearMemoryCache(key: String) {
        memoryCache.remove(key)
    }
}
