package com.gululu.aamediamate

import android.content.Context
import androidx.core.content.edit
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONArray
import org.json.JSONObject
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class SettingsManagerBridgedAppsTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("media_bridge_settings", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        MediaInformationRetriever.labelMap.clear()
    }

    @Test
    fun `addOrUpdateBridgedApp keeps existing label when label resolution falls back to package name`() {
        val packageName = "com.example.music"

        SettingsManager.addOrUpdateBridgedApp(context, packageName, "Music App")
        MediaInformationRetriever.labelMap.clear()

        mockkObject(MediaInformationRetriever)
        try {
            every { MediaInformationRetriever.getAppLabel(context, packageName) } returns packageName

            SettingsManager.addOrUpdateBridgedApp(context, packageName, packageName)

            val apps = SettingsManager.getBridgedApps(context)
            assertEquals(1, apps.size)
            assertEquals("Music App", apps.single().appName)
            verify(exactly = 0) { MediaInformationRetriever.getAppLabel(context, packageName) }
        } finally {
            unmockkObject(MediaInformationRetriever)
        }
    }

    @Test
    fun `addOrUpdateBridgedApp ignores self package`() {
        SettingsManager.addOrUpdateBridgedApp(context, context.packageName, "AAMediaMate")

        assertEquals(0, SettingsManager.getBridgedApps(context).size)
    }

    @Test
    fun `getBridgedApps filters stored self package`() {
        val selfPackage = context.packageName
        val storedApps = JSONArray().put(
            JSONObject().apply {
                put("packageName", selfPackage)
                put("appName", "AAMediaMate")
                put("firstSeen", 1L)
                put("lastSeen", 2L)
            }
        )

        context.getSharedPreferences("media_bridge_settings", Context.MODE_PRIVATE).edit {
            putString("bridged_apps", storedApps.toString())
        }

        assertEquals(0, SettingsManager.getBridgedApps(context).size)
    }
}
