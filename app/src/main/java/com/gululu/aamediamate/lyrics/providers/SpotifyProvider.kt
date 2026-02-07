package com.gululu.aamediamate.lyrics.providers

import android.content.Context
import android.util.Log
import com.gululu.aamediamate.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

object SpotifyProvider : LyricsProvider {
    private const val BASE_URL = "https://spotify-web-api3.p.rapidapi.com/v1/social/spotify/musixmatchsearchlyrics"

    private val client = OkHttpClient()

    override suspend fun getLyricsLrc(context: Context, title: String, artist: String, duration: String): String? = withContext(Dispatchers.IO) {
        val apiKey = SettingsManager.getApiKey(context)
        if (apiKey.isEmpty())
        {
            return@withContext null
        }

        try {
            val t = URLEncoder.encode(title, "UTF-8")
            val a = URLEncoder.encode(artist, "UTF-8")
            val url = "$BASE_URL?terms=$t&artist=$a"

            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("x-rapidapi-host", "spotify-web-api3.p.rapidapi.com")
                .addHeader("x-rapidapi-key", apiKey)
                .build()
            Log.d("MediaBridge", "Sending request to spotify: $request")

            val response = client.newCall(request).execute()

            Log.d("MediaBridge", "Response: $response")
            if (response.code() != 200) {
                return@withContext null
            }

            val body = response.body()?.string()

            if (body.isNullOrBlank()) {
                return@withContext null
            }

            // Parse JSON and extract data array
            val jsonObject = JSONObject(body)
            val dataArray = jsonObject.optJSONArray("data")

            if (dataArray == null || dataArray.length() == 0) {
                return@withContext null
            }

            // Extract lyrics lines and join with newlines
            val lyricsLines = mutableListOf<String>()
            for (i in 0 until dataArray.length()) {
                val line = dataArray.optString(i)
                if (line.isNotBlank()) {
                    lyricsLines.add(line)
                }
            }

            return@withContext lyricsLines.joinToString("\n").takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}