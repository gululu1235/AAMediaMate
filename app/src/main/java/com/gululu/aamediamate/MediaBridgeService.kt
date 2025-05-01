package com.gululu.aamediamate

import android.os.Bundle
import android.util.Log
import android.support.v4.media.MediaBrowserCompat
import androidx.media.MediaBrowserServiceCompat

class MediaBridgeService : MediaBrowserServiceCompat() {

    override fun onCreate() {
        super.onCreate()

        MediaBridgeSessionManager.init(this)

        sessionToken = MediaBridgeSessionManager.getSessionToken()!!

        val mediaInfo = MediaInformationRetriever.refreshCurrentMediaInfo(this)
        if (mediaInfo != null)
        {
            MediaBridgeSessionManager.updateFromMediaInfo(mediaInfo)
        }

        Log.d("MediaBridge", "MediaBrowserServiceCompat started")
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        return BrowserRoot("root", null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        val items = mutableListOf<MediaBrowserCompat.MediaItem>()

        result.sendResult(items)
    }
}
