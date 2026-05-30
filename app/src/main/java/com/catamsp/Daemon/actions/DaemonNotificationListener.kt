package com.catamsp.Daemon.actions

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.catamsp.Daemon.Application

class DaemonNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        updateNotifications()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        updateNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        updateNotifications()
    }

    private fun updateNotifications() {
        try {
            val active = getActiveNotifications()
            val packages = active?.mapNotNull { it.packageName }?.toSet() ?: emptySet()
            (applicationContext as? Application)?.activeNotifications?.postValue(packages)
        } catch (e: Exception) {
            Log.d("NotificationListener", "Failed to update notifications", e)
        }
    }
}
