package com.gululu.aamediamate

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.gululu.aamediamate.R
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MediaInformationRetrieverTest {

    private val context = mockk<Context>()
    private val packageManager = mockk<PackageManager>()
    private val sharedPreferences = mockk<SharedPreferences>(relaxed = true)

    @Before
    fun setUp() {
        MediaInformationRetriever.labelMap.clear()
        every { context.packageManager } returns packageManager
        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences
        every { sharedPreferences.getString(any(), any()) } answers { secondArg() }
    }

    @Test
    fun `getAppLabel uses alternate label when direct label matches package name`() {
        val packageName = "com.example.music"
        val appInfo = mockk<ApplicationInfo>()
        val fallbackAppInfo = mockk<ApplicationInfo>()
        val packageInfo = PackageInfo().apply {
            javaClass.getField("packageName").set(this, packageName)
            javaClass.getField("applicationInfo").set(this, fallbackAppInfo)
        }

        every { context.packageName } returns "com.gululu.aamediamate"
        every { packageManager.getApplicationInfo(packageName, 0) } returns appInfo
        every { packageManager.getApplicationLabel(appInfo) } returns packageName
        every { packageManager.getInstalledPackages(0) } returns listOf(packageInfo)
        every { fallbackAppInfo.loadLabel(packageManager) } returns "Music App"

        val label = MediaInformationRetriever.getAppLabel(context, packageName)

        assertEquals("Music App", label)
    }

    @Test
    fun `getAppLabel does not resolve self package as app name`() {
        val packageName = "com.gululu.aamediamate"
        val appInfo = mockk<ApplicationInfo>()

        every { context.packageName } returns packageName
        every { context.getString(R.string.unknown_app) } returns "Unknown App"
        every { packageManager.getApplicationInfo(packageName, 0) } returns appInfo
        every { packageManager.getApplicationLabel(appInfo) } returns "AAMediaMate"

        val label = MediaInformationRetriever.getAppLabel(context, packageName)

        assertEquals("Unknown App", label)
    }

    @Test
    fun `getAppLabel falls back to package name for unresolved external package`() {
        val packageName = "com.example.hiddenmusic"

        every { context.packageName } returns "com.gululu.aamediamate"
        every { packageManager.getApplicationInfo(packageName, 0) } throws PackageManager.NameNotFoundException()

        val label = MediaInformationRetriever.getAppLabel(context, packageName)

        assertEquals(packageName, label)
    }
}
