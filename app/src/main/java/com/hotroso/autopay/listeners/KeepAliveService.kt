package com.hotroso.autopay.listeners

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.hotroso.autopay.R
import com.hotroso.autopay.SettingActivity
import com.hotroso.autopay.common.Constants

class KeepAliveService : Service() {

    companion object {
        const val ACTION_STOP = "com.hotroso.autopay.STOP_SERVICE"
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(1, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(true)
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        sendRestartBroadcast()
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)
        sendRestartBroadcast()
    }

    private fun sendRestartBroadcast() {
        val intent = Intent(applicationContext, NotificationServiceRestartReceiver::class.java)
        intent.action = Constants.MY_ACTION_ON_REBOOT
        sendBroadcast(intent)
    }

    private fun createNotification(): Notification {
        val channelId = Constants.NOTIFICATION_CHANNEL_ID
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(channelId, Constants.NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW).apply {
            setShowBadge(false)
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)

        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, SettingActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return Notification.Builder(this, channelId)
            .setContentTitle("NotiRelay")
            .setContentText("Đang lắng nghe thông báo...")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
