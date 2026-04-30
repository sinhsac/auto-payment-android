package com.hotroso.autopay.workers

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.hotroso.autopay.common.Constants
import com.hotroso.autopay.models.XInfo
import java.io.OutputStreamWriter
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class PushingWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        val json = inputData.getString("xx_data") ?: return Result.failure()
        val info = Gson().fromJson(json, XInfo::class.java)
        Log.d("PushingWorker", "${info.getPck()} :: ${info.getTitle()}")

        val prefs = applicationContext.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
        val serverUrl = prefs.getString(Constants.PREF_SERVER_URL, Constants.DEFAULT_SERVER_URL)
            ?: Constants.DEFAULT_SERVER_URL

        return try {
            val code = httpPost(serverUrl, json)
            Log.d("PushingWorker", "Response: $code")
            if (code in 200..299) Result.success() else Result.retry()
        } catch (e: Exception) {
            Log.e("PushingWorker", "Error: ${e.message}")
            Result.retry()
        }
    }

    private fun httpPost(urlStr: String, json: String): Int {
        val conn = URL(urlStr).openConnection() as HttpsURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        conn.doOutput = true
        conn.connect()
        OutputStreamWriter(conn.outputStream, "UTF-8").use {
            it.write(json)
            it.flush()
        }
        return conn.responseCode
    }
}
