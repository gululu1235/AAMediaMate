package com.gululu.aamediamate

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.PlaybackState
import android.util.Log
import com.gululu.aamediamate.models.MediaInfo
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.session.MediaController
import androidx.core.graphics.withTranslation
import androidx.core.graphics.scale
import androidx.core.graphics.createBitmap

object MediaInformationRetriever {
    private val iconMap = mutableMapOf<String, Bitmap?>()
    internal val labelMap = mutableMapOf<String, String>()

    fun refreshCurrentMediaInfo(context: Context): MediaInfo? {
        try {
            val controller = MediaControllerManager.getFirstController(context) ?: return null

            val metadata = controller.metadata ?: return null
            val state = controller.playbackState ?: return null

            val appIcon = getAppIconBitmap(context, controller.packageName)
            var albumArt = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            if (appIcon != null)
            {
                albumArt = composeAlbumArtWithAppIconFixed(albumArt, appIcon)
            }

            val mediaInfo = MediaInfo(
                appPackageName = controller.packageName,
                appIcon = appIcon,
                appName = getAppLabel(context, controller.packageName),
                title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: context.getString(R.string.unknown_title),
                artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)?.takeIf { it.isNotBlank() } ?: "",
                album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM)?.takeIf { it.isNotBlank() } ?: "",
                duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
                position = state.position,
                isPlaying = state.state == PlaybackState.STATE_PLAYING,
                albumArt = albumArt
            )

            Log.d("MediaBridge", "🔄 Updating media info：$mediaInfo")
            return mediaInfo
        } catch (e: Exception) {
            Log.e("MediaBridge", "⚠️ Updating media info failed： ${e.message}")
            return null
        }
    }

    fun buildMediaInfoFromController(context: Context, controller: MediaController): MediaInfo? {
        val metadata = controller.metadata ?: return null
        val state = controller.playbackState ?: return null

        val appIcon = getAppIconBitmap(context, controller.packageName)
        var albumArt = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)

        if (appIcon != null && albumArt != null) {
            albumArt = composeAlbumArtWithAppIconFixed(albumArt, appIcon)
        }

        return MediaInfo(
            appPackageName = controller.packageName,
            appIcon = appIcon,
            appName = getAppLabel(context, controller.packageName),
            title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: context.getString(R.string.unknown_title),
            artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)?.takeIf { it.isNotBlank() } ?: "",
            album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM)?.takeIf { it.isNotBlank() } ?: "",
            duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION),
            position = state.position,
            isPlaying = state.state == PlaybackState.STATE_PLAYING,
            albumArt = albumArt
        )
    }

    private fun composeAlbumArtWithAppIconFixed(
        albumArt: Bitmap,
        appIcon: Bitmap,
        outputSize: Int = 512,      // album size 512x512
        appIconRatio: Float = 0.25f // app icon size
    ): Bitmap {
        val scaledAlbumArt = albumArt.scale(outputSize, outputSize)

        val resultBitmap = createBitmap(outputSize, outputSize)
        val canvas = Canvas(resultBitmap)

        canvas.drawBitmap(scaledAlbumArt, 0f, 0f, null)

        val appIconSize = (outputSize * appIconRatio).toInt()
        val scaledAppIcon = appIcon.scale(appIconSize, appIconSize)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            setShadowLayer(8f, 0f, 0f, Color.BLACK)
        }

        val iconLeft = outputSize - appIconSize - 16
        val iconTop = outputSize - appIconSize - 16

        canvas.withTranslation(iconLeft.toFloat(), iconTop.toFloat()) {
            val rect = RectF(0f, 0f, appIconSize.toFloat(), appIconSize.toFloat())
            drawRoundRect(rect, 20f, 20f, paint)
        }

        canvas.drawBitmap(scaledAppIcon, iconLeft.toFloat(), iconTop.toFloat(), null)

        return resultBitmap
    }

    fun getAppLabel(context: Context, packageName: String): String {
        return labelMap[packageName]?.also {
            Log.d("MediaBridge", "📱 Using cached app name for $packageName: $it")
        } ?: run {
            Log.d("MediaBridge", "📱 Fetching app name for $packageName from PackageManager")
            val appName = try {
                val pm = context.packageManager
                
                // Try the normal approach first
                val appInfo = pm.getApplicationInfo(packageName, 0)
                val label = pm.getApplicationLabel(appInfo).toString()
                
                // Validate the result - if it's the same as package name, try alternative
                if (label == packageName) {
                    Log.d("MediaBridge", "📱 Label equals package name, trying alternative for $packageName")
                    // Try getting installed packages approach
                    val packages = pm.getInstalledPackages(0)
                    val targetPackage = packages.find { it.packageName == packageName }
                    targetPackage?.applicationInfo?.loadLabel(pm)?.toString() ?: packageName
                } else {
                    label
                }
            } catch (e: Exception) {
                when (e) {
                    is android.content.pm.PackageManager.NameNotFoundException -> {
                        Log.w("MediaBridge", "⚠️ App not found: $packageName (possibly modded/uninstalled app)")
                    }
                    else -> {
                        Log.w("MediaBridge", "⚠️ Failed to get app name for $packageName: ${e.message}")
                    }
                }
                
                // Try one more alternative approach for missing packages
                try {
                    val pm = context.packageManager
                    val packages = pm.getInstalledPackages(0)
                    val targetPackage = packages.find { it.packageName == packageName }
                    targetPackage?.applicationInfo?.loadLabel(pm)?.toString() ?: packageName
                } catch (e2: Exception) {
                    Log.w("MediaBridge", "⚠️ All attempts failed for $packageName, using package name")
                    packageName
                }
            }
            // Cache the retrieved app name
            labelMap[packageName] = appName
            Log.d("MediaBridge", "📱 Cached app name for $packageName: $appName")
            appName
        }
    }

    private fun getAppIconBitmap(context: Context, packageName: String): Bitmap? {
        return iconMap[packageName] ?: run {
            val bitmap = try {
                val drawable = context.packageManager.getApplicationIcon(packageName)
                drawableToBitmap(drawable)
            } catch (e: Exception) {
                Log.w("MediaBridge", "⚠️ Failed to get app icon for $packageName: ${e.message}")
                null
            }
            // Cache the result (even if null) to avoid repeated attempts
            iconMap[packageName] = bitmap
            bitmap
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            drawable.bitmap?.let { return it }
        }
        val bitmap = createBitmap(drawable.intrinsicWidth.takeIf { it > 0 } ?: 1,
            drawable.intrinsicHeight.takeIf { it > 0 } ?: 1)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}