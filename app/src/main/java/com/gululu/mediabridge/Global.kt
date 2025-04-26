package com.gululu.mediabridge

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

object Global
{
    val allowedPackages = listOf(
        "com.tencent.qqmusic" to "QQ 音乐",
        "app.revanced.android.youtube" to "Youtube",
        "me.tangke.gamecores" to "机核",
    )

    fun packageAllowed(context: Context, packageName:String): Boolean {
        return !SettingsManager.getIgnoreNativeAutoApps(context) || !isAndroidAutoApp(context, packageName)
    }

    private fun isAndroidAutoApp(context: Context, packageName: String): Boolean {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            val hasCarMeta = appInfo.metaData?.containsKey("com.google.android.gms.car.application") ?: false
            Log.d("MediaBridge", "$packageName 是否为 Auto App: $hasCarMeta")
            hasCarMeta
        } catch (e: Exception) {
            false
        }
    }
}
