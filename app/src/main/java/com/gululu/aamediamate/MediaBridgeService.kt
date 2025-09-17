package com.gululu.aamediamate

import android.os.Bundle
import android.util.Log
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import androidx.media.MediaBrowserServiceCompat

class MediaBridgeService : MediaBrowserServiceCompat() {
    
    companion object {
        private var instance: MediaBridgeService? = null
        
        fun refreshBrowserData() {
            instance?.let { service ->
                Log.d("MediaBridge", "🔄 Refreshing browser data")
                service.notifyChildrenChanged("root")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        MediaBridgeSessionManager.init(this)

        sessionToken = MediaBridgeSessionManager.getSessionToken()!!

        val mediaInfo = MediaInformationRetriever.refreshCurrentMediaInfo(this)
        if (mediaInfo != null)
        {
            MediaBridgeSessionManager.updateFromMediaInfo(mediaInfo)
        }

        Log.d("MediaBridge", "MediaBrowserServiceCompat started")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
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
        Log.d("MediaBridge", "🔄 onLoadChildren called for parentId: $parentId")

        val context = this.applicationContext
        val controllers = MediaControllerManager.getAllControllers(context)

        val items = controllers.mapNotNull { controller ->
            val mediaInfo = MediaInformationRetriever.buildMediaInfoFromController(context, controller)
            
            // Only show apps that have active media info
            mediaInfo?.let {
                val description = MediaDescriptionCompat.Builder()
                    .setMediaId(controller.packageName)
                    .setTitle(it.appName)
                    .setSubtitle(it.title)
                    .setIconBitmap(it.appIcon)
                    .build()

                MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
            }
        }.toMutableList()

        Log.d("MediaBridge", "📋 Loaded ${items.size} media items")
        result.sendResult(items)
    }
}
