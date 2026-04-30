package com.hotroso.autopay.workers

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.hotroso.autopay.common.Constants
import com.hotroso.autopay.db.AppDatabase
import com.hotroso.autopay.models.NotificationEntity
import java.io.OutputStreamWriter
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class PushingWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
        val serverUrl  = prefs.getString(Constants.PREF_SERVER_URL, Constants.DEFAULT_SERVER_URL) ?: Constants.DEFAULT_SERVER_URL
        val authType   = prefs.getString(Constants.PREF_AUTH_TYPE, Constants.DEFAULT_AUTH_TYPE) ?: Constants.DEFAULT_AUTH_TYPE
        val authUser   = prefs.getString(Constants.PREF_AUTH_USER, "") ?: ""
        val authPass   = prefs.getString(Constants.PREF_AUTH_PASS, "") ?: ""
        val authSecret = prefs.getString(Constants.PREF_AUTH_SECRET, Constants.DEFAULT_AUTH_SECRET) ?: Constants.DEFAULT_AUTH_SECRET
        val authToken  = prefs.getString(Constants.PREF_AUTH_TOKEN, "") ?: ""

        val dao = AppDatabase.get(applicationContext).notificationDao()
        val pending = dao.getPending()

        if (pending.isEmpty()) return Result.success()

        val syncedIds = mutableListOf<Long>()
        for (entity in pending) {
            try {
                val code = httpPost(serverUrl, Gson().toJson(entity), authType, authUser, authPass, authSecret, authToken)
                if (code in 200..299) {
                    syncedIds.add(entity.id)
                } else {
                    Log.w("PushingWorker", "Server error $code for id=${entity.id}")
                }
            } catch (e: Exception) {
                Log.e("PushingWorker", "Failed id=${entity.id}: ${e.message}")
            }
        }

        if (syncedIds.isNotEmpty()) {
            dao.markSynced(syncedIds)
            dao.clearSynced()
        }

        return if (syncedIds.size == pending.size) Result.success() else Result.retry()
    }

    private fun httpPost(
        urlStr: String, json: String,
        authType: String, user: String, pass: String, secret: String, token: String
    ): Int {
        val conn = URL(urlStr).openConnection() as HttpsURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        conn.doOutput = true

        when (authType) {
            "basic"  -> {
                val encoded = Base64.encodeToString("$user:$pass".toByteArray(), Base64.NO_WRAP)
                conn.setRequestProperty("Authorization", "Basic $encoded")
            }
            "secret" -> conn.setRequestProperty("X-Auth-Secret", secret)
            "token"  -> conn.setRequestProperty("X-Auth-Token", token)
        }

        conn.connect()
        OutputStreamWriter(conn.outputStream, "UTF-8").use {
            it.write(json)
            it.flush()
        }
        return conn.responseCode
    }
}
