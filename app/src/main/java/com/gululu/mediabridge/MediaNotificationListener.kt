package com.gululu.mediabridge

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log

class MediaNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val title = extras.getString("android.title")
        val artist = extras.getString("android.text")

        Log.d("MediaBridge", "🎵 正在播放: $title - $artist")
        LogBuffer.append("收到播放通知: $title - $artist")

        if (sbn.notification.category == Notification.CATEGORY_TRANSPORT) {
            MediaBridgeSessionManager.updateMetadata(title, artist)
            MediaBridgeSessionManager.setPlaybackState(PlaybackStateCompat.STATE_PLAYING)
        }
    }
    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        Log.d("MediaBridge", "通知移除: ${sbn.packageName}")
    }
}
