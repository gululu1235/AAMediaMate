package com.gululu.aamediamate

import android.content.Context
import androidx.core.content.edit
import com.gululu.aamediamate.models.LanguageOption

object SettingsManager {
    private const val PREFS_NAME = "media_bridge_settings"
    private const val KEY_LYRICS_ENABLED = "lyrics_enabled"
    private const val KEY_API_KEY = "api_key"
    private const val KEY_SIMPLIFY = "simplify_chinese"
    private const val KEY_IGNORE_NATIVE_AUTO_APPS = "ignore_native_auto_apps"
    private const val KEY_LRC_API_URI = "lrc_api_uri"
    private const val KEY_LANGUAGE = "language_pref"

    private fun getPrefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getLyricsEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_LYRICS_ENABLED, false)

    fun setLyricsEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit() { putBoolean(KEY_LYRICS_ENABLED, enabled) }
    }

    fun getApiKey(context: Context): String =
        getPrefs(context).getString(KEY_API_KEY, "") ?: ""

    fun setApiKey(context: Context, key: String) {
        getPrefs(context).edit() { putString(KEY_API_KEY, key) }
    }

    fun getLrcApiBaseUri(context: Context): String =
        getPrefs(context).getString(KEY_LRC_API_URI, "") ?: ""

    fun setLrcApiBaseUri(context: Context, uri: String) {
        getPrefs(context).edit() { putString(KEY_LRC_API_URI, uri) }
    }

    fun getSimplifyEnabled(context: Context): Boolean {
        val prefs = getPrefs(context)
        if (prefs.contains(KEY_SIMPLIFY)) {
            return prefs.getBoolean(KEY_SIMPLIFY, true)
        }

        val locale = context.resources.configuration.locales.get(0)
        val isTraditionalChinese = locale.language == "zh" && locale.script == "Hant"

        return !isTraditionalChinese
    }

    fun setSimplifyEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit() { putBoolean(KEY_SIMPLIFY, enabled) }
    }

    fun getIgnoreNativeAutoApps(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_IGNORE_NATIVE_AUTO_APPS, true)

    fun setIgnoreNativeAutoApps(context: Context, enabled: Boolean) {
        getPrefs(context).edit() { putBoolean(KEY_IGNORE_NATIVE_AUTO_APPS, enabled) }
    }


    fun getLanguagePreference(context: Context): String =
        getPrefs(context).getString(KEY_LANGUAGE, "") ?: ""

    fun setLanguagePreference(context: Context, language: LanguageOption) {
        getPrefs(context).edit(commit = true) {
            putString(
                KEY_LANGUAGE,
                "${language.language}_${language.country}"
            )
        }
    }
}