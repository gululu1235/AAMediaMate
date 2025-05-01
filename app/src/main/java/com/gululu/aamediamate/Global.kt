package com.gululu.aamediamate

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

object Global
{
    fun packageAllowed(context: Context, packageName:String): Boolean {
        return !SettingsManager.getIgnoreNativeAutoApps(context) || !isAndroidAutoApp(context, packageName)
    }

    private fun isAndroidAutoApp(context: Context, packageName: String): Boolean {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            val hasCarMeta = appInfo.metaData?.containsKey("com.google.android.gms.car.application") ?: false
            hasCarMeta
        } catch (e: Exception) {
            false
        }
    }
}
