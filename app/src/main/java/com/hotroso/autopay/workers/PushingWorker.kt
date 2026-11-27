package com.hotroso.autopay.workers

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.hotroso.autopay.models.XInfo
import com.hotroso.autopay.utils.AutoPayPrefs
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class PushingWorker(
    private val appContext: Context,
    workerParameters: WorkerParameters
) : Worker(appContext, workerParameters) {

    override fun doWork(): Result {
        val json = inputData.getString(KEY_PAYLOAD_JSON)
        if (json.isNullOrBlank()) {
            return Result.failure()
        }

        val webhookUrl = AutoPayPrefs.getWebhookUrl(appContext)
        if (webhookUrl.isBlank()) {
            AutoPayPrefs.appendLog(appContext, "FAIL: Webhook URL is not configured")
            return Result.failure()
        }

        val secret = AutoPayPrefs.getWebhookSecret(appContext)
        val info = try {
            Gson().fromJson(json, XInfo::class.java)
        } catch (_: Exception) {
            null
        }
        val orderLabel = info?.orderCode ?: "unknown"

        return try {
            val (code, body) = postJson(webhookUrl, json, secret)
            Log.i(TAG, "Webhook response for $orderLabel -> HTTP $code: $body")

            when {
                code in 200..299 -> {
                    AutoPayPrefs.appendLog(appContext, "SENT $orderLabel -> HTTP $code OK")
                    Result.success()
                }
                code in 400..499 -> {
                    AutoPayPrefs.appendLog(appContext, "REJECTED $orderLabel -> HTTP $code")
                    Result.failure()
                }
                else -> {
                    AutoPayPrefs.appendLog(appContext, "RETRY $orderLabel -> HTTP $code (attempt ${runAttemptCount + 1})")
                    if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Webhook error for $orderLabel", e)
            AutoPayPrefs.appendLog(appContext, "ERROR $orderLabel -> ${e.message ?: "network error"}")
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val TAG = "AutoPayWorker"
        private const val MAX_RETRIES = 5
        const val KEY_PAYLOAD_JSON = "xx_data"

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
