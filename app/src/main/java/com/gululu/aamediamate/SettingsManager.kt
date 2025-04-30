package com.gululu.aamediamate

import android.content.Context
import androidx.core.content.edit

object SettingsManager {
    private const val PREFS_NAME = "media_bridge_settings"
    private const val KEY_LYRICS_ENABLED = "lyrics_enabled"
    private const val KEY_API_KEY = "api_key"
    private const val KEY_SIMPLIFY = "simplify_chinese"
    private const val KEY_IGNORE_NATIVE_AUTO_APPS = "ignore_native_auto_apps"
    private const val KEY_LRCCX_URI = "lrccx_uri"

    private fun getPrefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getLyricsEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_LYRICS_ENABLED, true)

    fun setLyricsEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit() { putBoolean(KEY_LYRICS_ENABLED, enabled) }
    }

    fun getApiKey(context: Context): String =
        getPrefs(context).getString(KEY_API_KEY, "") ?: ""

    fun setApiKey(context: Context, key: String) {
        getPrefs(context).edit() { putString(KEY_API_KEY, key) }
    }

    fun getLrcCxBaseUri(context: Context): String =
        getPrefs(context).getString(KEY_LRCCX_URI, "") ?: ""

    fun setLrcCxBaseUri(context: Context, uri: String) {
        getPrefs(context).edit() { putString(KEY_LRCCX_URI, uri) }
    }

    fun getSimplifyEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_SIMPLIFY, true)

    fun setSimplifyEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit() { putBoolean(KEY_SIMPLIFY, enabled) }
    }

    fun getIgnoreNativeAutoApps(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_IGNORE_NATIVE_AUTO_APPS, true)

    fun setIgnoreNativeAutoApps(context: Context, enabled: Boolean) {
        getPrefs(context).edit() { putBoolean(KEY_IGNORE_NATIVE_AUTO_APPS, enabled) }
    }
}