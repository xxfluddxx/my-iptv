package com.myiptv.carlauncher

import android.service.notification.NotificationListenerService

/**
 * Android only shares other apps' media sessions with an enabled notification
 * listener, so this exists purely to unlock [MediaSessions]. It ignores notifications.
 */
class MediaAccessService : NotificationListenerService()
