package com.gululu.aamediamate.lyrics.providers

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

object LRCLibProvider : LyricsProvider {
    private const val BASE_URL = "https://lrclib.net/api/get"
    private val client = OkHttpClient()

    override suspend fun getLyricsLrc(context: Context, title: String, artist: String, duration: String): String? = withContext(Dispatchers.IO) {
        try {
            val trackName = URLEncoder.encode(title, "UTF-8")
            val artistName = URLEncoder.encode(artist, "UTF-8")
            
            val url = "$BASE_URL?track_name=$trackName&artist_name=$artistName"

            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("User-Agent", "AAMediaMate/1.0")
                .build()

            Log.d("MediaBridge", "Sending request to LRCLib: $request")

            val response = client.newCall(request).execute()

            Log.d("MediaBridge", "LRCLib Response: $response")
            if (response.code() != 200) {
                return@withContext null
            }

            val body = response.body()?.string()
            if (body.isNullOrBlank()) {
                return@withContext null
            }

            // Parse JSON response and extract syncedLyrics
            val jsonObject = JSONObject(body)
            val syncedLyrics = jsonObject.optString("syncedLyrics", "")
            
            // Return synced lyrics if available, otherwise null
            if (syncedLyrics.isNotBlank()) syncedLyrics else null
        } catch (e: Exception) {
            Log.e("MediaBridge", "Error fetching lyrics from LRCLib", e)
            null
        }
    }
}