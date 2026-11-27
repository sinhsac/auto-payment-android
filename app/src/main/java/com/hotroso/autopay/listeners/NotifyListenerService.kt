package com.hotroso.autopay.listeners

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.hotroso.autopay.common.Constants
import com.hotroso.autopay.data.PaymentDatabaseHelper
import com.hotroso.autopay.data.PaymentRecord
import com.hotroso.autopay.data.SyncStatus
import com.hotroso.autopay.utils.AutoPayPrefs
import com.hotroso.autopay.utils.NotificationUtils
import com.hotroso.autopay.utils.PaymentNotificationParser
import com.hotroso.autopay.workers.PushingWorker
import com.hotroso.autopay.workers.SyncPendingWorker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotifyListenerService : NotificationListenerService() {

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

        if (!parsed.shouldForward || parsed.orderCode == null || parsed.amount == null) {
            Log.d(TAG, "Ignored notification from $pkg: ${parsed.reason}")
            return
        }

        val strPostTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).format(Date(sbn.postTime))
        val groupKey = sbn.groupKey ?: ""
        val fingerprint = PaymentDatabaseHelper.buildFingerprint(pkg, strPostTime, groupKey, message)

        // Persist locally to SQLite FIRST so no payment is ever lost if the server is offline
        val db = PaymentDatabaseHelper.getInstance(this)
        val record = PaymentRecord(
            fingerprint = fingerprint,
            orderCode = parsed.orderCode,
            amount = parsed.amount,
            bankName = parsed.bankName,
            packageName = pkg,
            title = title,
            rawMessage = message,
            groupKey = groupKey,
            postTimeMs = sbn.postTime,
            strPostTime = strPostTime,
            syncStatus = SyncStatus.PENDING
        )

        val rowId = db.insertIfNew(record)
        if (rowId <= 0L) {
            Log.d(TAG, "Duplicate payment notification ignored: ${parsed.orderCode}")
            return
        }

        Log.i(TAG, "Saved payment #$rowId (${parsed.orderCode}, +${parsed.amount} VND) -> queueing webhook sync")
        PushingWorker.enqueueForPayment(this, rowId, parsed.orderCode)
    }

    override fun onCreate() {
        super.onCreate()
        SyncPendingWorker.schedulePeriodicSync(this)
        Log.d(TAG, "NotifyListenerService created & periodic SQLite sync scheduled")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    companion object {
        private const val TAG = "AutoPayListener"
    }
}
