package com.gululu.mediabridge

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.PlaybackState
import android.util.Log
import com.gululu.mediabridge.models.MediaInfo
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.withTranslation

object MediaInformationRetriever {
    private val iconMap = mutableMapOf<String, Bitmap?>()
    private val labelMap = mutableMapOf<String, String>()

    fun refreshCurrentMediaInfo(context: Context): MediaInfo? {
        try {
            val sessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as android.media.session.MediaSessionManager
            val component = ComponentName(context, MediaNotificationListener::class.java)
            val controller = sessionManager.getActiveSessions(component)
                .firstOrNull { it.packageName != context.packageName && Global.packageAllowed(context, it.packageName) } ?: return null

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
                title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "未知标题",
                artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "未知艺术家",
                album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: "未知专辑",
                duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
                position = state.position,
                isPlaying = state.state == PlaybackState.STATE_PLAYING,
                albumArt = albumArt
            )

            Log.d("MediaBridge", "🔄 刷新媒体信息：$mediaInfo")
            return mediaInfo
        } catch (e: Exception) {
            Log.e("MediaBridge", "⚠️ 刷新媒体信息失败: ${e.message}")
            return null
        }
    }

    fun composeAlbumArtWithAppIconFixed(
        albumArt: Bitmap,
        appIcon: Bitmap,
        outputSize: Int = 512,      // 统一封面尺寸 512x512
        appIconRatio: Float = 0.25f // app icon 大小是封面宽度的 1/4
    ): Bitmap {
        // 1. 先统一封面图大小
        val scaledAlbumArt = Bitmap.createScaledBitmap(albumArt, outputSize, outputSize, true)

        // 2. 创建目标 bitmap
        val resultBitmap = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)

        // 3. 把封面图画上去
        canvas.drawBitmap(scaledAlbumArt, 0f, 0f, null)

        // 4. 处理 app icon
        val appIconSize = (outputSize * appIconRatio).toInt()
        val scaledAppIcon = Bitmap.createScaledBitmap(appIcon, appIconSize, appIconSize, true)

        // 5. 给 app icon 加阴影效果
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            setShadowLayer(8f, 0f, 0f, Color.BLACK) // 阴影半径8px，黑色
        }

        // 6. 画 app icon + 阴影
        val iconLeft = outputSize - appIconSize - 16 // 稍微多留一点 padding
        val iconTop = outputSize - appIconSize - 16

        // 6.1 在 canvas 上启用阴影
        canvas.withTranslation(iconLeft.toFloat(), iconTop.toFloat()) {
            // 画阴影
            val rect = RectF(0f, 0f, appIconSize.toFloat(), appIconSize.toFloat())
            drawRoundRect(rect, 20f, 20f, paint)
        }

        // 6.2 再画真正的 app icon
        canvas.drawBitmap(scaledAppIcon, iconLeft.toFloat(), iconTop.toFloat(), null)

        return resultBitmap
    }

    private fun getAppLabel(context: Context, packageName: String): String {
        return labelMap[packageName] ?: run {
            try {
                val pm = context.packageManager
                val appInfo = pm.getApplicationInfo(packageName, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                packageName
            }
        }
    }

    private fun getAppIconBitmap(context: Context, packageName: String): Bitmap? {
        return iconMap[packageName] ?: run {
            try {
                val drawable = context.packageManager.getApplicationIcon(packageName)
                drawableToBitmap(drawable)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            drawable.bitmap?.let { return it }
        }
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth.takeIf { it > 0 } ?: 1,
            drawable.intrinsicHeight.takeIf { it > 0 } ?: 1,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}