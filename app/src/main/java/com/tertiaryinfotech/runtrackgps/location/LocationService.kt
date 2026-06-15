package com.tertiaryinfotech.runtrackgps.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.tertiaryinfotech.runtrackgps.MainActivity
import com.tertiaryinfotech.runtrackgps.R

/**
 * Foreground service that keeps the process alive (and location flowing) while a
 * run is tracked with the screen locked. It owns no location logic itself — the
 * persistent notification is what Android requires for background GPS.
 *
 * This is the Android counterpart to iOS's `UIBackgroundModes: location`.
 */
class LocationService : android.app.Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startInForeground()
        return START_STICKY
    }

    private fun startInForeground() {
        createChannel()
        val tapIntent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            this, 0, tapIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Tracking your run…")
            .setSmallIcon(R.drawable.ic_stat_run)
            .setOngoing(true)
            .setContentIntent(pending)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                mgr.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID, "Run tracking", NotificationManager.IMPORTANCE_LOW,
                    ).apply { description = "Keeps GPS tracking active during a run." },
                )
            }
        }
    }

    companion object {
        private const val CHANNEL_ID = "run_tracking"
        private const val NOTIF_ID = 1001
    }
}
