package com.gululu.aamediamate.backup

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.gululu.aamediamate.SettingsManager
import com.gululu.aamediamate.lyrics.LyricCache
import com.gululu.aamediamate.lyrics.LyricsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupExportResult(
    val lyricsFileCount: Int,
    val includesSettings: Boolean
)

data class BackupPreview(
    val createdAt: Long,
    val appVersionName: String,
    val includesLyrics: Boolean,
    val includesSettings: Boolean,
    val includesSecrets: Boolean,
    val lyricsFileCount: Int
)

data class BackupRestoreResult(
    val restoredLyricsCount: Int,
    val restoredSettings: Boolean
)

object BackupManager {
    private const val FORMAT_VERSION = 1
    private const val MANIFEST_ENTRY = "manifest.json"
    private const val SETTINGS_ENTRY = "settings.json"
    private const val LYRICS_PREFIX = "lyrics/"

    fun createDefaultBackupFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
        return "AAMediaMate-backup-$timestamp.zip"
    }

    suspend fun createBackup(
        context: Context,
        outputUri: Uri,
        includeLyrics: Boolean,
        includeSettings: Boolean,
        includeSecrets: Boolean
    ): BackupExportResult = withContext(Dispatchers.IO) {
        val lyricsFiles = if (includeLyrics) getNonEmptyLyricsFiles(context) else emptyList()
        val settingsJson = if (includeSettings) {
            SettingsManager.exportBackupSettings(context, includeSecrets)
        } else {
            null
        }
        val manifest = JSONObject().apply {
            put("formatVersion", FORMAT_VERSION)
            put("createdAt", System.currentTimeMillis())
            put("appVersionName", getAppVersionName(context))
            put("includesLyrics", includeLyrics)
            put("includesSettings", includeSettings)
            put("includesSecrets", includeSettings && includeSecrets)
            put("lyricsFileCount", lyricsFiles.size)
        }

        val outputStream = context.contentResolver.openOutputStream(outputUri)
            ?: throw IOException("Could not open backup output file")

        ZipOutputStream(BufferedOutputStream(outputStream)).use { zip ->
            zip.writeJsonEntry(MANIFEST_ENTRY, manifest)

            if (settingsJson != null) {
                zip.writeJsonEntry(SETTINGS_ENTRY, settingsJson)
            }

            lyricsFiles.forEach { file ->
                zip.putNextEntry(ZipEntry("$LYRICS_PREFIX${file.name}"))
                file.inputStream().use { input ->
                    input.copyTo(zip)
                }
                zip.closeEntry()
            }
        }

        BackupExportResult(
            lyricsFileCount = lyricsFiles.size,
            includesSettings = includeSettings
        )
    }

    suspend fun readBackupPreview(context: Context, inputUri: Uri): BackupPreview = withContext(Dispatchers.IO) {
        val manifest = readManifest(context, inputUri)
        val formatVersion = manifest.optInt("formatVersion", -1)
        if (formatVersion != FORMAT_VERSION) {
            throw IOException("Unsupported backup format")
        }

        BackupPreview(
            createdAt = manifest.optLong("createdAt", 0L),
            appVersionName = manifest.optString("appVersionName", ""),
            includesLyrics = manifest.optBoolean("includesLyrics", false),
            includesSettings = manifest.optBoolean("includesSettings", false),
            includesSecrets = manifest.optBoolean("includesSecrets", false),
            lyricsFileCount = manifest.optInt("lyricsFileCount", 0)
        )
    }

    suspend fun restoreBackup(
        context: Context,
        inputUri: Uri,
        restoreLyrics: Boolean,
        restoreSettings: Boolean
    ): BackupRestoreResult = withContext(Dispatchers.IO) {
        var restoredLyricsCount = 0
        var restoredSettings = false
        val lyricsDir = LyricCache.getLyricsDir(context)
        val updatedKeys = mutableListOf<String>()

        val inputStream = context.contentResolver.openInputStream(inputUri)
            ?: throw IOException("Could not open backup input file")

        ZipInputStream(BufferedInputStream(inputStream)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    when {
                        restoreSettings && entry.name == SETTINGS_ENTRY -> {
                            val settings = JSONObject(zip.readCurrentEntryText())
                            SettingsManager.importBackupSettings(context, settings)
                            restoredSettings = true
                        }
                        restoreLyrics && entry.name.startsWith(LYRICS_PREFIX) -> {
                            val fileName = entry.name.removePrefix(LYRICS_PREFIX)
                            if (isSafeLyricsFileName(fileName)) {
                                val outputFile = File(lyricsDir, fileName)
                                outputFile.parentFile?.mkdirs()
                                outputFile.outputStream().use { output ->
                                    zip.copyTo(output)
                                }
                                restoredLyricsCount++
                                updatedKeys += fileName.removeSuffix(".lrt")
                            }
                        }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        updatedKeys.forEach { key ->
            LyricsRepository.notifyLyricsUpdated(key)
        }

        BackupRestoreResult(
            restoredLyricsCount = restoredLyricsCount,
            restoredSettings = restoredSettings
        )
    }

    private fun readManifest(context: Context, inputUri: Uri): JSONObject {
        val inputStream = context.contentResolver.openInputStream(inputUri)
            ?: throw IOException("Could not open backup input file")

        ZipInputStream(BufferedInputStream(inputStream)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name == MANIFEST_ENTRY) {
                    return JSONObject(zip.readCurrentEntryText())
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        throw IOException("Missing backup manifest")
    }

    private fun getNonEmptyLyricsFiles(context: Context): List<File> {
        val lyricsDir = LyricCache.getLyricsDir(context)
        return lyricsDir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".lrt") && it.length() > 0L }
            ?.sortedBy { it.name.lowercase(Locale.US) }
            ?: emptyList()
    }

    private fun getAppVersionName(context: Context): String {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        return packageInfo.versionName ?: ""
    }

    private fun isSafeLyricsFileName(fileName: String): Boolean =
        fileName.isNotBlank() &&
                fileName.endsWith(".lrt") &&
                !fileName.contains("/") &&
                !fileName.contains("\\")

    private fun ZipOutputStream.writeJsonEntry(name: String, json: JSONObject) {
        putNextEntry(ZipEntry(name))
        write(json.toString(2).toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun ZipInputStream.readCurrentEntryText(): String =
        readCurrentEntryBytes().toString(Charsets.UTF_8)

    private fun ZipInputStream.readCurrentEntryBytes(): ByteArray {
        val output = ByteArrayOutputStream()
        copyTo(output)
        return output.toByteArray()
    }
}
