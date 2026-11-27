package com.hotroso.autopay.listeners

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.gson.Gson
import com.hotroso.autopay.common.Constants
import com.hotroso.autopay.models.XInfo
import com.hotroso.autopay.utils.AutoPayPrefs
import com.hotroso.autopay.utils.NotificationUtils
import com.hotroso.autopay.utils.PaymentNotificationParser
import com.hotroso.autopay.workers.PushingWorker
import java.util.concurrent.TimeUnit

class NotifyListenerService : NotificationListenerService() {

    private val gson = Gson()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)

        if (!AutoPayPrefs.isListeningEnabled(this)) {
            return
        }

        val pkg = sbn.packageName ?: return
        if (pkg == packageName || Constants.UNSUPPORTED_APPS.contains(pkg)) {
            return
        }

        val title = NotificationUtils.getTitleRaw(sbn)
        val message = NotificationUtils.getMessage(sbn)
        if (message.isBlank()) {
            return
        }

        val prefix = AutoPayPrefs.getOrderPrefix(this)
        val onlyKnownBanks = AutoPayPrefs.isOnlyKnownBankApps(this)
        val parsed = PaymentNotificationParser.parse(pkg, title, message, prefix, onlyKnownBanks)

        if (!parsed.shouldForward || parsed.orderCode == null) {
            Log.d(TAG, "Ignored notification from $pkg: ${parsed.reason}")
            return
        }

        Log.i(TAG, "Matched payment notification: order=${parsed.orderCode}, amount=${parsed.amount}, bank=${parsed.bankName}")
        AutoPayPrefs.appendLog(this, "Matched ${parsed.orderCode} (+${parsed.amount} VND) from ${parsed.bankName}")

        val info = XInfo(sbn, extractedOrderCode = parsed.orderCode, extractedAmount = parsed.amount)
        enqueuePaymentWebhook(info, sbn.postTime)
    }

    private fun enqueuePaymentWebhook(info: XInfo, postTime: Long) {
        val data = Data.Builder()
            .putString(PushingWorker.KEY_PAYLOAD_JSON, gson.toJson(info))
            .build()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<PushingWorker>()
            .setInputData(data)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .addTag("auto-payment-webhook")
            .build()

        // Unique work name per matched payment so concurrent notifications never overwrite each other
        val uniqueName = "autopay_${info.orderCode ?: "unknown"}_$postTime"
        WorkManager.getInstance(this)
            .enqueueUniqueWork(uniqueName, ExistingWorkPolicy.KEEP, workRequest)
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "NotifyListenerService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    companion object {
        private const val TAG = "AutoPayListener"
    }
}
