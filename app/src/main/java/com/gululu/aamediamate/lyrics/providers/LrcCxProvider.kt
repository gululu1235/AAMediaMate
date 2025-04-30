package com.gululu.aamediamate.lyrics.providers

import android.content.Context
import android.util.Log
import com.gululu.aamediamate.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

object LrcCxProvider : LyricsProvider {
    private val client = OkHttpClient()

    override suspend fun getLyricsLrc(context: Context, title: String, artist: String, duration: String): String? = withContext(Dispatchers.IO) {
        val baseUrl = SettingsManager.getLrcCxBaseUri(context);
        if (baseUrl.isEmpty())
        {
            return@withContext null
        }

        try {
            val t = URLEncoder.encode(title, "UTF-8")
            val a = URLEncoder.encode(artist, "UTF-8")

            val url = "$baseUrl?title=$t&artist=$a"

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            Log.d("MediaBridge", "Sending request to LrcCx: $request")
            val response = client.newCall(request).execute()
            Log.d("MediaBridge", "Response: $response")

            if (response.code() != 200) {
                return@withContext null
            }

            val body = response.body()?.string()

            body?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}