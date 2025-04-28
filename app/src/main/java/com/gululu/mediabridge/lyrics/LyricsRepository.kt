package com.gululu.mediabridge.lyrics

import android.content.Context
import com.gululu.mediabridge.models.LyricsEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object LyricsRepository {
    suspend fun getAllLyrics(context: Context): List<LyricsEntry> = withContext(Dispatchers.IO) {
        val lyricsDir = LyricCache.getLyricsDir(context)
        if (!lyricsDir.exists()) return@withContext emptyList()

        lyricsDir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".lrt") }
            ?.map { file ->
                val key = file.name.removeSuffix(".lrt")
                val (title, artist) = key.split("_", limit = 2).let {
                    if (it.size == 2) it else listOf(key, "")
                }
                LyricsEntry(
                    key = key,
                    title = title,
                    artist = artist,
                    hasLyrics = file.length() > 0
                )
            }
            ?.sortedBy { it.title }
            ?: emptyList()
    }

    suspend fun deleteLyrics(context: Context, keys: List<String>) = withContext(Dispatchers.IO) {
        val lyricsDir = LyricCache.getLyricsDir(context)
        for (key in keys) {
            val file = File(lyricsDir, "$key.lrt")
            if (file.exists()) {
                file.delete()
            }
            LyricCache.clearMemoryCache(key)
        }
    }

    suspend fun loadLyricsText(context: Context, key: String): String {
        val lyricsDir = LyricCache.getLyricsDir(context)
        val file = File(lyricsDir, "$key.lrt")
        return if (file.exists()) {
            file.readText()
        } else {
            ""
        }
    }

    suspend fun saveLyricsText(context: Context, key: String, content: String) {
        val lyricsDir = LyricCache.getLyricsDir(context)
        val file = File(lyricsDir, "$key.lrt")
        file.parentFile?.mkdirs()
        file.writeText(content)
        LyricCache.clearMemoryCache(key)
    }
}
