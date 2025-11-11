package com.gululu.aamediamate.lyrics

import android.content.Context
import com.gululu.aamediamate.models.LyricsEntry
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

    suspend fun shiftLyricsByMs(context: Context, keys: List<String>, deltaMs: Long) = withContext(Dispatchers.IO) {
        if (keys.isEmpty()) return@withContext

        val pattern = Regex("\\[(\\\\d+):(\\\\d+(?:\\\\.\\\\d+)?)]")

        fun formatTime(totalSec: Float): String {
            val clamped = if (totalSec < 0f) 0f else totalSec
            val minutes = kotlin.math.floor((clamped / 60f).toDouble()).toInt()
            val seconds = clamped - minutes * 60f
            // format with two decimals
            return String.format("[%02d:%05.2f]", minutes, seconds)
        }

        val lyricsDir = LyricCache.getLyricsDir(context)

        keys.forEach { key ->
            val file = File(lyricsDir, "$key.lrt")
            if (!file.exists() || file.length() == 0L) return@forEach

            val original = file.readText()
            val shifted = original.lineSequence().joinToString("\n") { line ->
                pattern.replace(line) { match ->
                    val min = match.groupValues[1].toIntOrNull() ?: return@replace match.value
                    val sec = match.groupValues[2].toFloatOrNull() ?: return@replace match.value
                    val total = min * 60f + sec + (deltaMs / 1000f)
                    formatTime(total)
                }
            }

            file.writeText(shifted)
            LyricCache.clearMemoryCache(key)
        }
    }
}
