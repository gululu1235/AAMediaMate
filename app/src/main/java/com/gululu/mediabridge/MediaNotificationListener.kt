package com.gululu.mediabridge

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.gululu.mediabridge.models.MediaInfo

class MediaNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.notification.category != Notification.CATEGORY_TRANSPORT) return

        val packageName = sbn.packageName
        Log.d("MediaBridge", "📥 来自 $packageName 的媒体通知")

        Handler(Looper.getMainLooper()).postDelayed({
            MediaBridgeSessionManager.updateFromMediaInfo(MediaInformationRetriever.refreshCurrentMediaInfo(this))
        }, 500)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        MediaBridgeSessionManager.updateFromMediaInfo(
            MediaInformationRetriever.refreshCurrentMediaInfo(this))
        Log.d("MediaBridge", "通知移除: ${sbn.packageName}")
    }
}
