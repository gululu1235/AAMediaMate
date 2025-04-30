package com.gululu.aamediamate

import android.app.Notification
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

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
