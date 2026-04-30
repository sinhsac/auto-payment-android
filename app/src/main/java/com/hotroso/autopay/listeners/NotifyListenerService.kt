package com.hotroso.autopay.listeners

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.hotroso.autopay.common.Constants
import com.hotroso.autopay.db.AppDatabase
import com.hotroso.autopay.models.NotificationEntity
import com.hotroso.autopay.utils.NotificationUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.hotroso.autopay.workers.PushingWorker

class NotifyListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        if (Constants.UNSUPPORTED_APPS.contains(sbn.packageName)) return

        val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
        val entity = NotificationEntity(
            pck      = sbn.packageName,
            title    = NotificationUtils.getTitleRaw(sbn),
            msg      = NotificationUtils.getMessage(sbn),
            groupKey = sbn.groupKey,
            postTime = df.format(Date(sbn.postTime))
        )

        Thread {
            AppDatabase.get(applicationContext).notificationDao().insert(entity)
            enqueueSync()
        }.start()
    }

    private fun enqueueSync() {
        val request = OneTimeWorkRequest.Builder(PushingWorker::class.java)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork("noti-sync", ExistingWorkPolicy.KEEP, request)
    }

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }
}
