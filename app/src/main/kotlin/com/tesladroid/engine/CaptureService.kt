package com.tesladroid.engine

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaCodec
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.tesladroid.MainActivity
import com.tesladroid.R

class CaptureService : Service() {
    @JvmField
    var displayMode: String = "settings"

    @JvmField
    var activeCodec: MediaCodec? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            val notification = buildForegroundNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            StreamServer.isRunning = true
        } catch (e: Throwable) {
            android.util.Log.w("CaptureService", "startForeground error: ${e.message}")
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        StreamServer.isRunning = false
        if (instance == this) {
            instance = null
        }
    }

    fun updateDisplayMode(mode: String) {
        displayMode = mode
    }

    fun broadcastPhoneConfig() {}

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "TeslaDroid Capture Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Screen capture and display streaming"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TeslaDroid")
            .setContentText("Display streaming active")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "tesladroid_capture_channel"
        const val NOTIFICATION_ID = 4043

        @JvmField
        var instance: CaptureService? = null
    }
}
