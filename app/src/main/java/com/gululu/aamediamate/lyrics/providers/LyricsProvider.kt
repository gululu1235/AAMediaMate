package com.gululu.aamediamate.lyrics.providers

import android.content.Context

interface LyricsProvider {
    suspend fun getLyricsLrc(context: Context, title: String, artist: String, duration: String): String?
}