package com.gululu.aamediamate.data

enum class LyricsCleanupField {
    ARTIST,
    TITLE
}

data class LyricsCleanupRule(
    val id: String,
    val name: String,
    val field: LyricsCleanupField,
    val pattern: String,
    val isEnabled: Boolean = true
)
