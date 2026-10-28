package com.tesladroid.core.engine

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.tesladroid.MainActivity
import com.tesladroid.R

/**
 * Foreground Service running the streaming engine:
 * MediaProjection -> MediaCodec (H.264/H.265) -> Ktor / WebSocket server (port 8443)
 */
class TeslaStreamingService : Service() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, buildForegroundNotification())
                // In production, instantiate and bind MediaProjection to VirtualDisplay
                // and hardware MediaCodec encoder loop
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        CoreEngineBridge.instance.stopStreaming(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "TeslaDroid Stream Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active screen stream to Tesla vehicle browser"
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
            .setContentText("Port 8443 (WSS/HTTPS) • Video stream to Tesla")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "tesladroid_streaming_channel"
        const val NOTIFICATION_ID = 4042

        const val ACTION_START = "com.tesladroid.action.START_STREAM"
        const val ACTION_STOP = "com.tesladroid.action.STOP_STREAM"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_DATA = "extra_data"
        const val EXTRA_MODE = "extra_mode"
        const val EXTRA_QUALITY = "extra_quality"
        const val EXTRA_AUDIO_MODE = "extra_audio_mode"
    }
}
