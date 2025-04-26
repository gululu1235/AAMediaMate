package com.gululu.mediabridge.lyrics

import android.util.Log
import com.github.houbb.opencc4j.util.ZhConverterUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.regex.Pattern

data class LyricLine(val timeSec: Float, val text: String)

object LyricsManager {
    private const val API_KEY = "269d966f47mshf4eca5d853e4344p103bfdjsn11bdfe995261"
    private const val BASE_URL = "https://musixmatch-lyrics-songs.p.rapidapi.com/songs/lyrics"

    private val client = OkHttpClient()

    suspend fun getLyricsLrc(title: String, artist: String, duration: String): List<LyricLine> = withContext(Dispatchers.IO) {
        try {
            val t = URLEncoder.encode(title, "UTF-8")
            val a = URLEncoder.encode(artist, "UTF-8")
            val url = "$BASE_URL?t=$t&a=$a&type=text"

            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("x-rapidapi-host", "musixmatch-lyrics-songs.p.rapidapi.com")
                .addHeader("x-rapidapi-key", API_KEY)
                .build()

            Log.d("MediaBridge", "Sending request: $request")

            val response = client.newCall(request).execute()
            val body = response.body()?.string() ?: return@withContext emptyList()
            Log.d("MediaBridge", "Response: $response")

            return@withContext parseLrc(body)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }

    private fun parseLrc(lrc: String): List<LyricLine> {
        val pattern = Pattern.compile("\\[(\\d+):(\\d+\\.\\d+)](.*)")
        return lrc.lineSequence()
            .mapNotNull { line ->
                val matcher = pattern.matcher(line)
                if (matcher.find()) {
                    val min = matcher.group(1).toInt()
                    val sec = matcher.group(2).toFloat()
                    val text = matcher.group(3).trim()
                    val stext = zhConvertToSimplified(text)
                    LyricLine(timeSec = min * 60 + sec, text = stext)
                } else null
            }
            .sortedBy { it.timeSec }
            .toList()
    }

    private fun zhConvertToSimplified(text: String): String {
        return ZhConverterUtil.toSimple(text)
    }
}
