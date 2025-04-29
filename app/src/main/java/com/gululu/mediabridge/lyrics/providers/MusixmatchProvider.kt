package com.gululu.mediabridge.lyrics.providers

import android.content.Context
import android.util.Log
import com.gululu.mediabridge.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

object MusixmatchProvider : LyricsProvider {
    private const val BASE_URL = "https://musixmatch-lyrics-songs.p.rapidapi.com/songs/lyrics"

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
            val url = "$BASE_URL?t=$t&a=$a&type=text"

            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("x-rapidapi-host", "musixmatch-lyrics-songs.p.rapidapi.com")
                .addHeader("x-rapidapi-key", apiKey)
                .build()
            Log.d("MediaBridge", "Sending request to musixmatch: $request")

            val response = client.newCall(request).execute()

            Log.d("MediaBridge", "Response: $response")
            if (response.code() != 200) {
                return@withContext null
            }

            val body = response.body()?.string()


            return@withContext body?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}