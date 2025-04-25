package com.gululu.mediabridge

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import androidx.media.MediaBrowserServiceCompat

class MediaBridgeService : androidx.media.MediaBrowserServiceCompat() {

    override fun onCreate() {
        super.onCreate()
        Log.d("MediaBridge", "📡 MediaBridgeService 启动")

        MediaBridgeSessionManager.init(this)

        sessionToken = MediaBridgeSessionManager.getSessionToken()!!

        val mediaInfo = MediaInformationRetriever.refreshCurrentMediaInfo(this)
        if (mediaInfo != null)
        {
            MediaBridgeSessionManager.updateFromMediaInfo(mediaInfo)
        }

        Log.d("MediaBridge", "📡 MediaBrowserServiceCompat 启动")
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): MediaBrowserServiceCompat.BrowserRoot {
        Log.d("MediaBridge", "onGetRoot called from $clientPackageName")
        return BrowserRoot("root", null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        val items = mutableListOf<MediaBrowserCompat.MediaItem>()

//        items.addAll(
//            Global.allowedPackages.map { (pkg, label) ->
//                val desc = MediaDescriptionCompat.Builder()
//                    .setMediaId(pkg)
//                    .setTitle("启动 $label")
//                    .setSubtitle(pkg)
//                    .build()
//
//                MediaBrowserCompat.MediaItem(desc, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
//            }
//        )
//        Log.d("mediabridage", "sending result $items")
        result.sendResult(items)
    }

    private fun isWhitelistedPlaying(context: Context): Boolean {
        val sessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as android.media.session.MediaSessionManager
        val component = ComponentName(context, MediaNotificationListener::class.java)
        val controllers = sessionManager.getActiveSessions(component)

        return controllers.any {
            Global.packageAllowed(context, it.packageName)
        }
    }
}
