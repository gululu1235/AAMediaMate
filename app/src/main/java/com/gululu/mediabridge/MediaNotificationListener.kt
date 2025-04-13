package com.gululu.mediabridge

import android.app.Notification
import android.graphics.Bitmap
import android.os.Parcelable
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log

class MediaNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val title = extras.getString("android.title")
        val artist = extras.getString("android.text")

        var albumArt: Bitmap? = null

        try {
            val icon = extras.getParcelable<Parcelable>(Notification.EXTRA_LARGE_ICON)
            if (icon is Bitmap) {
                albumArt = icon
            } else {
                val picture = extras.getParcelable<Parcelable>(Notification.EXTRA_PICTURE)
                if (picture is Bitmap) {
                    albumArt = picture
                }
            }
        } catch (e: Exception) {
            LogBuffer.append("⚠️ 提取封面图失败: ${e.message}")
        }

        LogBuffer.append("📥 收到通知: $title - $artist")

        if (sbn.notification.category == Notification.CATEGORY_TRANSPORT) {
            MediaBridgeSessionManager.updateMetadata(title, artist, albumArt)
            MediaBridgeSessionManager.setPlaybackState(PlaybackStateCompat.STATE_PLAYING)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        Log.d("MediaBridge", "通知移除: ${sbn.packageName}")
    }
}
