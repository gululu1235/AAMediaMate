package com.gululu.aamediamate.lyrics

import com.gululu.aamediamate.data.LyricsCleanupField
import com.gululu.aamediamate.data.LyricsCleanupRule
import java.util.regex.PatternSyntaxException

object LyricsCleanupManager {
    fun applyRules(
        title: String,
        artist: String,
        rules: List<LyricsCleanupRule>,
    ): Pair<String, String> {
        var cleanedTitle = title
        var cleanedArtist = artist

        rules.filter { it.isEnabled }.forEach { rule ->
            when (rule.field) {
                LyricsCleanupField.ARTIST -> {
                    cleanedArtist = applyRule(cleanedArtist, rule.pattern)
                }
                LyricsCleanupField.TITLE -> {
                    cleanedTitle = applyRule(cleanedTitle, rule.pattern)
                }
            }
        }

        return cleanedTitle to cleanedArtist
    }

    private fun applyRule(value: String, pattern: String): String {
        if (pattern.isBlank() || value.isBlank()) {
            return value
        }

        return try {
            Regex(pattern).replace(value, "").trim()
        } catch (_: PatternSyntaxException) {
            value
        }
    }
}
