package com.hotroso.autopay.listeners

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.google.gson.Gson
import com.hotroso.autopay.common.Constants
import com.hotroso.autopay.models.XInfo
import com.hotroso.autopay.workers.PushingWorker

class NotifyListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        if (Constants.UNSUPPORTED_APPS.contains(sbn.packageName)) return
        enqueueWork(XInfo(sbn))
    }

    private fun enqueueWork(info: XInfo) {
        val data = Data.Builder()
            .putString("xx_data", Gson().toJson(info))
            .build()

        val request = OneTimeWorkRequest.Builder(PushingWorker::class.java)
            .setInputData(data)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(this)
            .enqueueUniqueWork("push-${System.currentTimeMillis()}", ExistingWorkPolicy.KEEP, request)
    }

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }
}
