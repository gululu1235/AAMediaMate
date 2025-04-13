package com.gululu.mediabridge

import android.os.Bundle
import android.util.Log
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import androidx.media.MediaBrowserServiceCompat

class MediaBridgeService : MediaBrowserServiceCompat() {

    override fun onCreate() {
        super.onCreate()

        // 初始化 MediaSession
        MediaBridgeSessionManager.init(this)

        // 绑定给 MediaBrowserService
        sessionToken = MediaBridgeSessionManager.getSessionToken()!!

        Log.d("MediaBridge", "📡 MediaBrowserServiceCompat 启动")
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        Log.d("MediaBridge", "onGetRoot called from $clientPackageName")
        LogBuffer.append("📡 onGetRoot called")
        return BrowserRoot("root", null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        val item = MediaDescriptionCompat.Builder()
            .setMediaId("placeholder1")
            .setTitle("Welcome to Media Bridge")
            .setSubtitle("From Gululu")
            .build()

        val mediaItem = MediaBrowserCompat.MediaItem(
            item,
            MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
        )

        result.sendResult(mutableListOf(mediaItem))
        LogBuffer.append("📤 返回媒体项: Hello World")
    }
}
