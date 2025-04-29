package com.gululu.mediabridge.models

data class LyricsEntry(
    val key: String,         // title_artist
    val title: String,
    val artist: String,
    val hasLyrics: Boolean   // true = 有歌词，false = 只有空文件
)