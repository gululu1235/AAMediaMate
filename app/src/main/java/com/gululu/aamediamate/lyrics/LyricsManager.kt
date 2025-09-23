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
        val pattern = Pattern.compile("\\[(\\d+):(\\d+\\.\\d+)](.*)")
        return lrc.lineSequence()
            .mapNotNull { line ->
                val matcher = pattern.matcher(line)
                if (matcher.find()) {
                    val min = matcher.group(1)!!.toInt()
                    val sec = matcher.group(2)!!.toFloat()
                    var text = matcher.group(3)!!.trim()
                    if (SettingsManager.getSimplifyEnabled(context))
                    {
                        text = ZhConverterUtil.toSimple(text)
                    }
                    else
                    {
                        text = ZhConverterUtil.toTraditional(text)
                    }
                    LyricLine(timeSec = min * 60 + sec, text = text)
                } else null
            }
            .sortedBy { it.timeSec }
            .toList()
    }
}
