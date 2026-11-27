package com.hotroso.autopay.workers

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.hotroso.autopay.data.PaymentDatabaseHelper
import com.hotroso.autopay.data.PaymentRecord
import com.hotroso.autopay.data.SyncStatus
import com.hotroso.autopay.models.XInfo
import com.hotroso.autopay.utils.AutoPayPrefs
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Date
import java.util.concurrent.TimeUnit

class PushingWorker(
    private val appContext: Context,
    workerParameters: WorkerParameters
) : Worker(appContext, workerParameters) {

    override fun doWork(): Result {
        val db = PaymentDatabaseHelper.getInstance(appContext)
        val paymentId = inputData.getLong(KEY_PAYMENT_ID, -1L)

        if (paymentId > 0L) {
            val record = db.getById(paymentId) ?: return Result.success()
            if (record.syncStatus == SyncStatus.SYNCED) {
                return Result.success()
            }
            return syncRecord(appContext, db, record, runAttemptCount)
        }

        // Legacy fallback if called with raw JSON payload
        val rawJson = inputData.getString(KEY_PAYLOAD_JSON) ?: return Result.failure()
        val webhookUrl = AutoPayPrefs.getWebhookUrl(appContext)
        if (webhookUrl.isBlank()) return Result.failure()
        return try {
            val (code, _) = postJson(webhookUrl, rawJson, AutoPayPrefs.getWebhookSecret(appContext))
            if (code in 200..299) Result.success() else Result.retry()
        } catch (_: Exception) {
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val TAG = "AutoPayWorker"
        private const val MAX_RETRIES = 5
        const val KEY_PAYMENT_ID = "payment_id"
        const val KEY_PAYLOAD_JSON = "xx_data"

        private val gson = Gson()

        fun enqueueForPayment(context: Context, paymentId: Long, orderCode: String) {
            val data = Data.Builder()
                .putLong(KEY_PAYMENT_ID, paymentId)
                .build()

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<PushingWorker>()
                .setInputData(data)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .addTag("auto-payment-webhook")
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork("autopay_record_${paymentId}_$orderCode", ExistingWorkPolicy.REPLACE, request)
        }

        fun syncRecord(
            context: Context,
            db: PaymentDatabaseHelper,
            record: PaymentRecord,
            attemptCount: Int = 0
        ): Result {
            val webhookUrl = AutoPayPrefs.getWebhookUrl(context)
            if (webhookUrl.isBlank()) {
                db.markFailed(record.id, null, "Chưa cấu hình URL Server Webhook")
                return Result.failure()
            }

            val secret = AutoPayPrefs.getWebhookSecret(context)
            val payload = XInfo(
                groupKey = record.groupKey,
                postTime = Date(record.postTimeMs),
                strPostTime = record.strPostTime,
                title = record.title,
                msg = record.rawMessage,
                pck = record.packageName,
                orderCode = record.orderCode,
                amount = record.amount
            )

            return try {
                val (code, body) = postJson(webhookUrl, gson.toJson(payload), secret)
                Log.i(TAG, "Webhook ${record.orderCode} -> HTTP $code: $body")

                when {
                    code in 200..299 -> {
                        val outcome = extractOutcome(body) ?: "OK"
                        db.markSynced(record.id, code, outcome)
                        AutoPayPrefs.appendLog(context, "ĐỒNG BỘ ${record.orderCode} -> $outcome (HTTP $code)")
                        Result.success()
                    }
                    code in 400..499 -> {
                        val err = "HTTP $code: ${body.take(120)}"
                        db.markFailed(record.id, code, err)
                        AutoPayPrefs.appendLog(context, "TỪ CHỐI ${record.orderCode} -> HTTP $code")
                        Result.failure()
                    }
                    else -> {
                        val err = "Lỗi server HTTP $code (lần ${attemptCount + 1})"
                        db.markFailed(record.id, code, err)
                        AutoPayPrefs.appendLog(context, "THỬ LẠI ${record.orderCode} -> HTTP $code")
                        if (attemptCount < MAX_RETRIES) Result.retry() else Result.failure()
                    }
                }
            } catch (e: Exception) {
                val err = e.message ?: "Không kết nối được server"
                Log.e(TAG, "Webhook error for ${record.orderCode}", e)
                db.markFailed(record.id, null, err)
                AutoPayPrefs.appendLog(context, "LỖI MẠNG ${record.orderCode} -> $err")
                if (attemptCount < MAX_RETRIES) Result.retry() else Result.failure()
            }
        }

        private fun extractOutcome(body: String): String? {
            return try {
                val obj = JsonParser.parseString(body).asJsonObject
                obj.get("outcome")?.asString ?: obj.get("orderStatus")?.asString
            } catch (_: Exception) {
                null
            }
        }

        fun postJson(targetUrl: String, jsonBody: String, webhookSecret: String = ""): Pair<Int, String> {
            val conn = (URL(targetUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 15_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                if (webhookSecret.isNotBlank()) {
                    setRequestProperty("X-Webhook-Secret", webhookSecret)
                }
            }

            return try {
                BufferedWriter(OutputStreamWriter(conn.outputStream, Charsets.UTF_8)).use { writer ->
                    writer.write(jsonBody)
                    writer.flush()
                }

                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val responseBody = if (stream != null) {
                    BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
                } else {
                    ""
                }
                code to responseBody
            } finally {
                conn.disconnect()
            }
        }
    }
}
