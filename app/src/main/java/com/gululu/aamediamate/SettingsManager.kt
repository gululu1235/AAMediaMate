package com.gululu.aamediamate

import android.content.Context
import androidx.core.content.edit
import com.gululu.aamediamate.data.LyricsCleanupField
import com.gululu.aamediamate.data.LyricsCleanupRule
import com.gululu.aamediamate.models.LanguageOption
import com.gululu.aamediamate.models.BridgedApp
import com.gululu.aamediamate.data.LyricsProviderConfig
import com.gululu.aamediamate.data.LyricsProviderRegistry
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object SettingsManager {
    private const val PREFS_NAME = "media_bridge_settings"
    private const val KEY_LYRICS_ENABLED = "lyrics_enabled"
    private const val KEY_API_KEY = "api_key"
    private const val KEY_SIMPLIFY = "simplify_chinese"
    private const val KEY_IGNORE_NATIVE_AUTO_APPS = "ignore_native_auto_apps"
    private const val KEY_LRC_API_URI = "lrc_api_uri"
    private const val KEY_LRC_API_AUTH_TOKEN = "lrc_api_auth_token"
    private const val KEY_LANGUAGE = "language_pref"
    private const val KEY_BRIDGED_APPS = "bridged_apps"
    private const val KEY_LYRICS_PROVIDERS = "lyrics_providers"
    private const val KEY_LYRICS_CLEANUP_RULES = "lyrics_cleanup_rules"
    private const val KEY_COMBINE_APP_ICON_AND_ALBUM_ART = "combine_app_icon_and_album_art"
    private const val KEY_SHOW_ALBUM_NAME = "show_album_name"
    private const val KEY_LYRICS_TIMING_OFFSET = "lyrics_timing_offset"

    private fun getPrefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun exportBackupSettings(context: Context, includeSecrets: Boolean): JSONObject {
        val prefs = getPrefs(context)

        return JSONObject().apply {
            put(KEY_LYRICS_ENABLED, getLyricsEnabled(context))
            put(KEY_SIMPLIFY, getSimplifyEnabled(context))
            put(KEY_IGNORE_NATIVE_AUTO_APPS, getIgnoreNativeAutoApps(context))
            put(KEY_LRC_API_URI, getLrcApiBaseUri(context))
            put(KEY_LANGUAGE, getLanguagePreference(context))
            put(KEY_BRIDGED_APPS, prefs.getJSONArrayString(KEY_BRIDGED_APPS))
            put(KEY_LYRICS_PROVIDERS, prefs.getJSONArrayString(KEY_LYRICS_PROVIDERS))
            put(KEY_LYRICS_CLEANUP_RULES, lyricsCleanupRulesToJson(getLyricsCleanupRules(context)))
            put(KEY_COMBINE_APP_ICON_AND_ALBUM_ART, getCombineAppIconAndAlbumArt(context))
            put(KEY_SHOW_ALBUM_NAME, getShowAlbumName(context))
            put(KEY_LYRICS_TIMING_OFFSET, getLyricsTimingOffset(context))

            if (includeSecrets) {
                put(KEY_API_KEY, getApiKey(context))
                put(KEY_LRC_API_AUTH_TOKEN, getLrcApiAuthToken(context))
            }
        }
    }

    fun importBackupSettings(context: Context, settings: JSONObject) {
        getPrefs(context).edit {
            settings.optBooleanOrNull(KEY_LYRICS_ENABLED)?.let { putBoolean(KEY_LYRICS_ENABLED, it) }
            settings.optBooleanOrNull(KEY_SIMPLIFY)?.let { putBoolean(KEY_SIMPLIFY, it) }
            settings.optBooleanOrNull(KEY_IGNORE_NATIVE_AUTO_APPS)?.let { putBoolean(KEY_IGNORE_NATIVE_AUTO_APPS, it) }
            settings.optStringOrNull(KEY_LRC_API_URI)?.let { putString(KEY_LRC_API_URI, it) }
            settings.optStringOrNull(KEY_LANGUAGE)?.let { putString(KEY_LANGUAGE, it) }
            settings.optJSONArrayStringOrNull(KEY_BRIDGED_APPS)?.let { putString(KEY_BRIDGED_APPS, it) }
            settings.optJSONArrayStringOrNull(KEY_LYRICS_PROVIDERS)?.let { putString(KEY_LYRICS_PROVIDERS, it) }
            settings.optJSONArrayStringOrNull(KEY_LYRICS_CLEANUP_RULES)?.let { putString(KEY_LYRICS_CLEANUP_RULES, it) }
            settings.optBooleanOrNull(KEY_COMBINE_APP_ICON_AND_ALBUM_ART)?.let {
                putBoolean(KEY_COMBINE_APP_ICON_AND_ALBUM_ART, it)
            }
            settings.optBooleanOrNull(KEY_SHOW_ALBUM_NAME)?.let { putBoolean(KEY_SHOW_ALBUM_NAME, it) }
            settings.optIntOrNull(KEY_LYRICS_TIMING_OFFSET)?.let { putInt(KEY_LYRICS_TIMING_OFFSET, it) }
            settings.optStringOrNull(KEY_API_KEY)?.let { putString(KEY_API_KEY, it) }
            settings.optStringOrNull(KEY_LRC_API_AUTH_TOKEN)?.let { putString(KEY_LRC_API_AUTH_TOKEN, it) }
        }
    }

    fun getCombineAppIconAndAlbumArt(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_COMBINE_APP_ICON_AND_ALBUM_ART, true)

    fun setCombineAppIconAndAlbumArt(context: Context, enabled: Boolean) {
        getPrefs(context).edit() { putBoolean(KEY_COMBINE_APP_ICON_AND_ALBUM_ART, enabled) }
    }

    fun getShowAlbumName(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_SHOW_ALBUM_NAME, true)

    fun setShowAlbumName(context: Context, enabled: Boolean) {
        getPrefs(context).edit() { putBoolean(KEY_SHOW_ALBUM_NAME, enabled) }
    }

    fun getLyricsTimingOffset(context: Context): Int =
        getPrefs(context).getInt(KEY_LYRICS_TIMING_OFFSET, 0)

    fun setLyricsTimingOffset(context: Context, offsetMs: Int) {
        getPrefs(context).edit() { putInt(KEY_LYRICS_TIMING_OFFSET, offsetMs) }
    }

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

    fun getLrcApiAuthToken(context: Context): String =
        getPrefs(context).getString(KEY_LRC_API_AUTH_TOKEN, "") ?: ""

    fun setLrcApiAuthToken(context: Context, token: String) {
        getPrefs(context).edit() { putString(KEY_LRC_API_AUTH_TOKEN, token) }
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
            if (packageName == context.packageName) continue

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
                    lyricsEnabled = jsonObject.optBoolean("lyricsEnabled", true),
                    headUnitControlEnabled = jsonObject.optBoolean("headUnitControlEnabled", true),
                    swapRewindFastForward = jsonObject.optBoolean("swapRewindFastForward", false)
                )
            )
        }
        return apps
    }

    fun getStoredBridgedAppName(context: Context, packageName: String): String? {
        if (packageName == context.packageName) return null

        val jsonString = getPrefs(context).getString(KEY_BRIDGED_APPS, "[]") ?: "[]"
        val jsonArray = JSONArray(jsonString)

        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.optJSONObject(i) ?: continue
            if (jsonObject.optString("packageName") == packageName) {
                val storedAppName = jsonObject.optString("appName", "")
                if (storedAppName.isNotBlank() && storedAppName != packageName) {
                    return storedAppName
                }
            }
        }

        return null
    }

    fun addOrUpdateBridgedApp(context: Context, packageName: String, appName: String) {
        if (packageName == context.packageName) return

        val apps = getBridgedApps(context).toMutableList()
        val existingIndex = apps.indexOfFirst { it.packageName == packageName }
        val existingAppName = apps.getOrNull(existingIndex)?.appName
        
        val finalAppName = when {
            appName.isNotBlank() && appName != packageName -> {
                // We already have a good app name, use it and cache it.
                MediaInformationRetriever.labelMap[packageName] = appName
                appName
            }
            !existingAppName.isNullOrBlank() && existingAppName != packageName -> {
                // Preserve a previously known good name instead of overwriting it
                // with a transient package-name fallback.
                existingAppName
            }
            else -> MediaInformationRetriever.getAppLabel(context, packageName)
        }

        if (finalAppName.isNotBlank() && finalAppName != packageName) {
            MediaInformationRetriever.labelMap[packageName] = finalAppName
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
                    lyricsEnabled = true,
                    headUnitControlEnabled = true,
                    swapRewindFastForward = false
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

    fun setAppHeadUnitControlEnabled(context: Context, packageName: String, enabled: Boolean) {
        val apps = getBridgedApps(context).toMutableList()
        val existingIndex = apps.indexOfFirst { it.packageName == packageName }

        if (existingIndex >= 0) {
            apps[existingIndex] = apps[existingIndex].copy(headUnitControlEnabled = enabled)
            saveBridgedApps(context, apps)
        }
    }

    fun isAppHeadUnitControlEnabled(context: Context, packageName: String): Boolean {
        return getBridgedApps(context).find { it.packageName == packageName }?.headUnitControlEnabled ?: true
    }

    fun setAppSwapRewindFastForward(context: Context, packageName: String, enabled: Boolean) {
        val apps = getBridgedApps(context).toMutableList()
        val existingIndex = apps.indexOfFirst { it.packageName == packageName }

        if (existingIndex >= 0) {
            apps[existingIndex] = apps[existingIndex].copy(swapRewindFastForward = enabled)
            saveBridgedApps(context, apps)
        }
    }

    fun isAppSwapRewindFastForward(context: Context, packageName: String): Boolean {
        return getBridgedApps(context).find { it.packageName == packageName }?.swapRewindFastForward ?: false
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
                put("headUnitControlEnabled", app.headUnitControlEnabled)
                put("swapRewindFastForward", app.swapRewindFastForward)
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
        val savedProviderOrder = mutableMapOf<String, Int>()
        val registryOrder = allProviders.mapIndexed { index, provider -> provider.id to index }.toMap()
        
        // Parse saved settings
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val id = jsonObject.getString("id")
            val baseProvider = LyricsProviderRegistry.getProviderById(id)
            savedProviderOrder[id] = i
            
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
        }.sortedWith(
            compareBy<LyricsProviderConfig> { it.priority }
                .thenBy { savedProviderOrder[it.id] ?: Int.MAX_VALUE }
                .thenBy { registryOrder[it.id] ?: Int.MAX_VALUE }
        ).normalizedProviderOrder()
    }

    fun saveLyricsProviders(context: Context, providers: List<LyricsProviderConfig>) {
        val jsonArray = JSONArray()
        providers.normalizedProviderOrder().forEach { provider ->
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
            val provider = providers.removeAt(index)
            val targetIndex = (priority - 1).coerceIn(0, providers.size)
            providers.add(targetIndex, provider)
            saveLyricsProviders(context, providers.normalizedProviderOrder())
        }
    }

    /** Moves a lyrics provider by one visible priority slot. */
    fun moveProviderPriority(context: Context, providerId: String, direction: Int) {
        val providers = getLyricsProviders(context)
        val currentIndex = providers.indexOfFirst { it.id == providerId }
        if (currentIndex < 0) return

        val targetIndex = (currentIndex + direction.coerceIn(-1, 1)).coerceIn(providers.indices)
        if (targetIndex == currentIndex) return

        updateProviderPriority(context, providerId, targetIndex + 1)
    }

    fun getEnabledProvidersInOrder(context: Context): List<LyricsProviderConfig> {
        return getLyricsProviders(context)
            .filter { it.isEnabled }
            .sortedBy { it.priority }
    }

    private fun List<LyricsProviderConfig>.normalizedProviderOrder(): List<LyricsProviderConfig> {
        return mapIndexed { index, provider ->
            provider.copy(priority = index + 1)
        }
    }

    fun getLyricsCleanupRules(context: Context): List<LyricsCleanupRule> {
        val prefs = getPrefs(context)
        if (!prefs.contains(KEY_LYRICS_CLEANUP_RULES)) {
            return defaultLyricsCleanupRules()
        }

        val jsonString = prefs.getString(KEY_LYRICS_CLEANUP_RULES, "[]") ?: "[]"
        val jsonArray = JSONArray(jsonString)
        val rules = mutableListOf<LyricsCleanupRule>()

        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.optJSONObject(i) ?: continue
            val field = runCatching {
                LyricsCleanupField.valueOf(jsonObject.optString("field", LyricsCleanupField.ARTIST.name))
            }.getOrDefault(LyricsCleanupField.ARTIST)

            rules.add(
                LyricsCleanupRule(
                    id = jsonObject.optString("id", UUID.randomUUID().toString()),
                    name = jsonObject.optString("name", ""),
                    field = field,
                    pattern = jsonObject.optString("pattern", ""),
                    isEnabled = jsonObject.optBoolean("isEnabled", true)
                )
            )
        }

        return rules
    }

    fun saveLyricsCleanupRules(context: Context, rules: List<LyricsCleanupRule>) {
        val jsonArray = JSONArray()
        rules.forEach { rule ->
            val jsonObject = JSONObject().apply {
                put("id", rule.id)
                put("name", rule.name)
                put("field", rule.field.name)
                put("pattern", rule.pattern)
                put("isEnabled", rule.isEnabled)
            }
            jsonArray.put(jsonObject)
        }

        getPrefs(context).edit {
            putString(KEY_LYRICS_CLEANUP_RULES, jsonArray.toString())
        }
    }

    private fun defaultLyricsCleanupRules(): List<LyricsCleanupRule> {
        return listOf(
            LyricsCleanupRule(
                id = "default_lossless_artist",
                name = "Lossless",
                field = LyricsCleanupField.ARTIST,
                pattern = """(?i)\s*[•·-]?\s*Lossless\s*$""",
                isEnabled = true
            )
        )
    }

    private fun android.content.SharedPreferences.getJSONArrayString(key: String): JSONArray {
        val value = getString(key, "[]") ?: "[]"
        return runCatching { JSONArray(value) }.getOrElse { JSONArray() }
    }

    private fun lyricsCleanupRulesToJson(rules: List<LyricsCleanupRule>): JSONArray {
        val jsonArray = JSONArray()
        rules.forEach { rule ->
            jsonArray.put(
                JSONObject().apply {
                    put("id", rule.id)
                    put("name", rule.name)
                    put("field", rule.field.name)
                    put("pattern", rule.pattern)
                    put("isEnabled", rule.isEnabled)
                }
            )
        }
        return jsonArray
    }

    private fun JSONObject.optBooleanOrNull(key: String): Boolean? =
        if (has(key) && !isNull(key)) optBoolean(key) else null

    private fun JSONObject.optIntOrNull(key: String): Int? =
        if (has(key) && !isNull(key)) optInt(key) else null

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (has(key) && !isNull(key)) optString(key, "") else null

    private fun JSONObject.optJSONArrayStringOrNull(key: String): String? {
        if (!has(key) || isNull(key)) return null

        val value = opt(key)
        return when (value) {
            is JSONArray -> value.toString()
            is String -> runCatching { JSONArray(value).toString() }.getOrNull()
            else -> null
        }
    }
}
