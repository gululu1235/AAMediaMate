package com.gululu.aamediamate.lyrics

import android.content.Context
import com.github.houbb.opencc4j.util.ZhConverterUtil
import com.gululu.aamediamate.SettingsManager
import com.gululu.aamediamate.lyrics.providers.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

data class LyricLine(val timeSec: Float, val text: String)

object LyricsManager {
    suspend fun getLyricsLrt(context: Context, title: String, artist: String, duration: String): String? = withContext(Dispatchers.IO) {
        // Get enabled providers in order of priority
        val enabledProviders = SettingsManager.getEnabledProvidersInOrder(context)
        
        for (providerConfig in enabledProviders) {
            try {
                val lrc = providerConfig.provider.getLyricsLrc(context, title, artist, duration)
                if (!lrc.isNullOrBlank()) {
                    return@withContext lrc
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return@withContext null
    }

    fun parseLrc(context: Context, lrc: String): List<LyricLine> {
        // This regex captures all timestamp tags at the beginning of the line, and the lyric text.
        // Group 1: The entire block of timestamp tags (e.g., "[00:01.23][00:02.45]")
        // Group 2: The lyric text after the timestamps
        val linePattern = Pattern.compile("((?:\\[\\d+:\\d+\\.\\d+])+)(.*)")

        // This regex is for parsing a single timestamp tag from the block captured by group 1.
        val timeTagPattern = Pattern.compile("\\[(\\d+):(\\d+\\.\\d+)]")

        val lyricLines = mutableListOf<LyricLine>()

        lrc.lineSequence().forEach { line ->
            val lineMatcher = linePattern.matcher(line)
            if (lineMatcher.matches()) {
                val tagsBlock = lineMatcher.group(1)!!
                var text = lineMatcher.group(2)!!.trim()

                // Chinese character conversion
                if (SettingsManager.getSimplifyEnabled(context)) {
                    text = ZhConverterUtil.toSimple(text)
                } else {
                    text = ZhConverterUtil.toTraditional(text)
                }

                val timeTagMatcher = timeTagPattern.matcher(tagsBlock)
                while (timeTagMatcher.find()) {
                    val min = timeTagMatcher.group(1)!!.toInt()
                    val sec = timeTagMatcher.group(2)!!.toFloat()
                    val timeSec = min * 60 + sec
                    lyricLines.add(LyricLine(timeSec = timeSec, text = text))
                }
            }
        }

        return lyricLines.sortedBy { it.timeSec }
    }
}
