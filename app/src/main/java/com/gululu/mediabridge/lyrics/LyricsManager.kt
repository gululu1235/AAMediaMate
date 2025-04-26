package com.gululu.mediabridge.lyrics

import android.content.Context
import android.util.Log
import com.github.houbb.opencc4j.util.ZhConverterUtil
import com.gululu.mediabridge.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.regex.Pattern

data class LyricLine(val timeSec: Float, val text: String)

object LyricsManager {
    private const val BASE_URL = "https://musixmatch-lyrics-songs.p.rapidapi.com/songs/lyrics"

    private val client = OkHttpClient()

    suspend fun getLyricsLrt(context: Context, title: String, artist: String, duration: String): String? = withContext(Dispatchers.IO) {
        try {
            val apiKey = SettingsManager.getApiKey(context)
            val t = URLEncoder.encode(title, "UTF-8")
            val a = URLEncoder.encode(artist, "UTF-8")
            val url = "$BASE_URL?t=$t&a=$a&type=text"

            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("x-rapidapi-host", "musixmatch-lyrics-songs.p.rapidapi.com")
                .addHeader("x-rapidapi-key", apiKey)
                .build()

            Log.d("MediaBridge", "Sending request: $request")

            val response = client.newCall(request).execute()
            val body = response.body()?.string()
            Log.d("MediaBridge", "Response: $response")

            return@withContext body?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    fun parseLrc(context: Context, lrc: String): List<LyricLine> {
        val pattern = Pattern.compile("\\[(\\d+):(\\d+\\.\\d+)](.*)")
        return lrc.lineSequence()
            .mapNotNull { line ->
                val matcher = pattern.matcher(line)
                if (matcher.find()) {
                    val min = matcher.group(1).toInt()
                    val sec = matcher.group(2).toFloat()
                    var text = matcher.group(3).trim()
                    if (SettingsManager.getSimplifyEnabled(context))
                    {
                        text = zhConvertToSimplified(text)
                    }
                    LyricLine(timeSec = min * 60 + sec, text = text)
                } else null
            }
            .sortedBy { it.timeSec }
            .toList()
    }

    private fun zhConvertToSimplified(text: String): String {
        return ZhConverterUtil.toSimple(text)
    }
}
