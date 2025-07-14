package com.gululu.aamediamate

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController

object MediaControllerManager {
    fun getAllControllers(context: Context): List<MediaController> {
        val sessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as android.media.session.MediaSessionManager
        val component = ComponentName(context, MediaNotificationListener::class.java)
        return sessionManager.getActiveSessions(component)
            .filter { it.packageName != context.packageName && Global.packageAllowed(context, it.packageName) }
    }

    fun getFirstController(context: Context): MediaController? {
        return getAllControllers(context).firstOrNull()
    }

    fun getActiveController(context: Context): MediaController? {
        val target = MediaBridgeSessionManager.getCurrentMediaPackage() ?: return null
        return getAllControllers(context).firstOrNull { it.packageName == target }
    }
}