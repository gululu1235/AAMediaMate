package com.gululu.aamediamate

import android.content.Context
import androidx.core.content.edit
import com.gululu.aamediamate.models.LanguageOption
import com.gululu.aamediamate.models.BridgedApp
import com.gululu.aamediamate.data.LyricsProviderConfig
import com.gululu.aamediamate.data.LyricsProviderRegistry
import org.json.JSONArray
import org.json.JSONObject

object SettingsManager {
    private const val PREFS_NAME = "media_bridge_settings"
    private const val KEY_LYRICS_ENABLED = "lyrics_enabled"
    private const val KEY_API_KEY = "api_key"
    private const val KEY_SIMPLIFY = "simplify_chinese"
    private const val KEY_IGNORE_NATIVE_AUTO_APPS = "ignore_native_auto_apps"
    private const val KEY_LRC_API_URI = "lrc_api_uri"
    private const val KEY_LANGUAGE = "language_pref"
    private const val KEY_BRIDGED_APPS = "bridged_apps"
    private const val KEY_LYRICS_PROVIDERS = "lyrics_providers"

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

    // Bridged Apps Management
    fun getBridgedApps(context: Context): List<BridgedApp> {
        val jsonString = getPrefs(context).getString(KEY_BRIDGED_APPS, "[]") ?: "[]"
        val jsonArray = JSONArray(jsonString)
        val apps = mutableListOf<BridgedApp>()
        
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val packageName = jsonObject.getString("packageName")
            val storedAppName = jsonObject.getString("appName")
            
            // If stored app name is just the package name, try to get proper app name again
            val refreshedAppName = if (storedAppName == packageName) {
                MediaInformationRetriever.getAppLabel(context, packageName)
            } else {
                storedAppName
            }
            
            apps.add(
                BridgedApp(
                    packageName = packageName,
                    appName = refreshedAppName,
                    firstSeen = jsonObject.getLong("firstSeen"),
                    lastSeen = jsonObject.getLong("lastSeen"),
                    lyricsEnabled = jsonObject.optBoolean("lyricsEnabled", true)
                )
            )
        }
        return apps
    }

    fun addOrUpdateBridgedApp(context: Context, packageName: String, appName: String) {
        val apps = getBridgedApps(context).toMutableList()
        val existingIndex = apps.indexOfFirst { it.packageName == packageName }
        
        // Use cached app name from MediaInformationRetriever if available
        // Only fallback to PackageManager if we don't have a good app name already
        val finalAppName = if (appName.isNotBlank() && appName != packageName) {
            // We already have a good app name, use it and cache it
            MediaInformationRetriever.labelMap[packageName] = appName
            appName
        } else {
            // Get app name from cache or PackageManager
            MediaInformationRetriever.getAppLabel(context, packageName)
        }
        
        if (existingIndex >= 0) {
            // Update existing app
            val existing = apps[existingIndex]
            apps[existingIndex] = existing.copy(
                appName = finalAppName, // Update app name in case it changed or was cached
                lastSeen = System.currentTimeMillis()
            )
        } else {
            // Add new app
            apps.add(
                BridgedApp(
                    packageName = packageName,
                    appName = finalAppName,
                    firstSeen = System.currentTimeMillis(),
                    lastSeen = System.currentTimeMillis(),
                    lyricsEnabled = true
                )
            )
        }
        
        saveBridgedApps(context, apps)
    }

    fun setAppLyricsEnabled(context: Context, packageName: String, enabled: Boolean) {
        val apps = getBridgedApps(context).toMutableList()
        val existingIndex = apps.indexOfFirst { it.packageName == packageName }
        
        if (existingIndex >= 0) {
            apps[existingIndex] = apps[existingIndex].copy(lyricsEnabled = enabled)
            saveBridgedApps(context, apps)
        }
    }

    fun isAppLyricsEnabled(context: Context, packageName: String): Boolean {
        return getBridgedApps(context).find { it.packageName == packageName }?.lyricsEnabled ?: true
    }

    private fun saveBridgedApps(context: Context, apps: List<BridgedApp>) {
        val jsonArray = JSONArray()
        apps.forEach { app ->
            val jsonObject = JSONObject().apply {
                put("packageName", app.packageName)
                put("appName", app.appName)
                put("firstSeen", app.firstSeen)
                put("lastSeen", app.lastSeen)
                put("lyricsEnabled", app.lyricsEnabled)
            }
            jsonArray.put(jsonObject)
        }
        
        getPrefs(context).edit {
            putString(KEY_BRIDGED_APPS, jsonArray.toString())
        }
    }

    // Lyrics Providers Management
    fun getLyricsProviders(context: Context): List<LyricsProviderConfig> {
        val jsonString = getPrefs(context).getString(KEY_LYRICS_PROVIDERS, "[]") ?: "[]"
        val jsonArray = JSONArray(jsonString)
        val allProviders = LyricsProviderRegistry.getAllProviders()
        val savedProviders = mutableMapOf<String, LyricsProviderConfig>()
        
        // Parse saved settings
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val id = jsonObject.getString("id")
            val baseProvider = LyricsProviderRegistry.getProviderById(id)
            
            if (baseProvider != null) {
                savedProviders[id] = baseProvider.copy(
                    isEnabled = jsonObject.optBoolean("isEnabled", baseProvider.isEnabled),
                    priority = jsonObject.optInt("priority", baseProvider.priority)
                )
            }
        }
        
        // Return all providers with saved settings applied, or defaults if not saved
        return allProviders.map { provider ->
            savedProviders[provider.id] ?: provider
        }.sortedBy { it.priority }
    }

    fun saveLyricsProviders(context: Context, providers: List<LyricsProviderConfig>) {
        val jsonArray = JSONArray()
        providers.forEach { provider ->
            val jsonObject = JSONObject().apply {
                put("id", provider.id)
                put("isEnabled", provider.isEnabled)
                put("priority", provider.priority)
            }
            jsonArray.put(jsonObject)
        }
        
        getPrefs(context).edit {
            putString(KEY_LYRICS_PROVIDERS, jsonArray.toString())
        }
    }

    fun updateProviderEnabled(context: Context, providerId: String, enabled: Boolean) {
        val providers = getLyricsProviders(context).toMutableList()
        val index = providers.indexOfFirst { it.id == providerId }
        if (index >= 0) {
            providers[index] = providers[index].copy(isEnabled = enabled)
            saveLyricsProviders(context, providers)
        }
    }

    fun updateProviderPriority(context: Context, providerId: String, priority: Int) {
        val providers = getLyricsProviders(context).toMutableList()
        val index = providers.indexOfFirst { it.id == providerId }
        if (index >= 0) {
            providers[index] = providers[index].copy(priority = priority)
            saveLyricsProviders(context, providers.sortedBy { it.priority })
        }
    }

    fun getEnabledProvidersInOrder(context: Context): List<LyricsProviderConfig> {
        return getLyricsProviders(context)
            .filter { it.isEnabled }
            .sortedBy { it.priority }
    }
}