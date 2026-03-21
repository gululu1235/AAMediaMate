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
        Log.d("MediaBridge", "📥 Media Notification from $packageName")

        sync()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        sync()
    }

    private fun sync()
    {
        Handler(Looper.getMainLooper()).postDelayed({
            MediaBridgeSessionManager.updateFromMediaInfo(MediaInformationRetriever.refreshCurrentMediaInfo(this))
            
            // Directly refresh browser data
            MediaBridgeService.refreshBrowserData()
        }, 1000)
    }
}
