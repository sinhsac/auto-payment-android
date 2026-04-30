package com.hotroso.autopay.listeners

import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.hotroso.autopay.common.Constants
import com.hotroso.autopay.db.AppDatabase
import com.hotroso.autopay.models.NotificationEntity
import com.hotroso.autopay.utils.NotificationUtils
import java.io.OutputStreamWriter
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.net.ssl.HttpsURLConnection

class NotifyListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("NotiRelay", "NotifyListenerService CONNECTED")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d("NotiRelay", "NotifyListenerService DISCONNECTED - requesting rebind")
        requestRebind(android.content.ComponentName(this, NotifyListenerService::class.java))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        Log.d("NotiRelay", "onNotificationPosted: ${sbn.packageName}")
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
            AppDatabase.get(this).notificationDao().insert(entity)
            Log.d("NotiRelay", "Inserted to DB, syncing...")
            syncToServer(entity)
        }.start()
    }

    private fun syncToServer(entity: NotificationEntity) {
        val prefs = getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
        val serverUrl  = prefs.getString(Constants.PREF_SERVER_URL, Constants.DEFAULT_SERVER_URL) ?: Constants.DEFAULT_SERVER_URL
        val authType   = prefs.getString(Constants.PREF_AUTH_TYPE, Constants.DEFAULT_AUTH_TYPE) ?: Constants.DEFAULT_AUTH_TYPE
        val authUser   = prefs.getString(Constants.PREF_AUTH_USER, "") ?: ""
        val authPass   = prefs.getString(Constants.PREF_AUTH_PASS, "") ?: ""
        val authSecret = prefs.getString(Constants.PREF_AUTH_SECRET, Constants.DEFAULT_AUTH_SECRET) ?: Constants.DEFAULT_AUTH_SECRET
        val authToken  = prefs.getString(Constants.PREF_AUTH_TOKEN, "") ?: ""

        try {
            val json = Gson().toJson(entity)
            Log.d("NotiRelay", "POSTing to $serverUrl")
            val conn = URL(serverUrl).openConnection() as HttpsURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.doOutput = true
            when (authType) {
                "basic"  -> conn.setRequestProperty("Authorization", "Basic " + Base64.encodeToString("$authUser:$authPass".toByteArray(), Base64.NO_WRAP))
                "secret" -> conn.setRequestProperty("X-Auth-Secret", authSecret)
                "token"  -> conn.setRequestProperty("X-Auth-Token", authToken)
            }
            conn.connect()
            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(json); it.flush() }
            val code = conn.responseCode
            Log.d("NotiRelay", "Server response: $code")
            if (code in 200..299) {
                AppDatabase.get(this).notificationDao().markSynced(listOf(entity.id))
                AppDatabase.get(this).notificationDao().clearSynced()
            }
        } catch (e: Exception) {
            Log.e("NotiRelay", "Sync failed: ${e.message}")
        }
    }

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }
}
