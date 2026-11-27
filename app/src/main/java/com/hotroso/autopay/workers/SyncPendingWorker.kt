package com.hotroso.autopay.workers

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.hotroso.autopay.data.PaymentDatabaseHelper
import java.util.concurrent.TimeUnit

/**
 * Periodically and on-demand syncs all PENDING or FAILED payment records stored
 * in the local SQLite database back to the auto-payment server once it recovers.
 */
class SyncPendingWorker(
    private val appContext: Context,
    workerParameters: WorkerParameters
) : Worker(appContext, workerParameters) {

    override fun doWork(): Result {
        val db = PaymentDatabaseHelper.getInstance(appContext)
        val unsynced = db.getPendingOrFailed(limit = 100)
        if (unsynced.isEmpty()) {
            return Result.success()
        }

        Log.i(TAG, "Syncing ${unsynced.size} pending/failed payment record(s) from local SQLite")
        var anyRetryableFailure = false

        for (record in unsynced) {
            val res = PushingWorker.syncRecord(appContext, db, record, record.retryCount)
            if (res == Result.retry()) {
                anyRetryableFailure = true
            }
        }

        return if (anyRetryableFailure) Result.retry() else Result.success()
    }

    companion object {
        private const val TAG = "AutoPaySync"
        private const val PERIODIC_WORK_NAME = "auto_pay_periodic_sync"
        private const val ONETIME_WORK_NAME = "auto_pay_manual_sync"

        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncPendingWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun enqueueNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncPendingWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(ONETIME_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
