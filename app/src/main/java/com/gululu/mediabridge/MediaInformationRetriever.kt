package com.gululu.mediabridge

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.PlaybackState
import android.util.Log
import com.gululu.mediabridge.models.MediaInfo

object MediaInformationRetriever {
    fun refreshCurrentMediaInfo(context: Context): MediaInfo? {
        try {
            val sessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as android.media.session.MediaSessionManager
            val component = ComponentName(context, MediaNotificationListener::class.java)
            val controller = sessionManager.getActiveSessions(component)
                .firstOrNull { it.packageName != context.packageName && Global.packageAllowed(context, it.packageName) } ?: return null

            val metadata = controller.metadata ?: return null
            val state = controller.playbackState ?: return null

            val mediaInfo = MediaInfo(
                appName = controller.packageName,
                title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "未知标题",
                artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "未知艺术家",
                album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: "未知专辑",
                duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
                position = state.position,
                isPlaying = state.state == PlaybackState.STATE_PLAYING,
                albumArt = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            )

            Log.d("MediaBridge", "🔄 刷新媒体信息：$mediaInfo")
            return mediaInfo
        } catch (e: Exception) {
            Log.e("MediaBridge", "⚠️ 刷新媒体信息失败: ${e.message}")
            return null
        }
    }
}