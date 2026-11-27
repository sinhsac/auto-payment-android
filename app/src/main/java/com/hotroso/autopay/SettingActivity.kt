package com.hotroso.autopay

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import com.google.gson.Gson
import com.hotroso.autopay.common.Constants
import com.hotroso.autopay.helpers.AutoStartHelper
import com.hotroso.autopay.models.XInfo
import com.hotroso.autopay.utils.AutoPayPrefs
import com.hotroso.autopay.utils.NotificationListenerUtil
import com.hotroso.autopay.workers.PushingWorker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingActivity : AppCompatActivity() {

    private lateinit var notificationListenerUtil: NotificationListenerUtil
    private lateinit var notiLisPerLauncher: ActivityResultLauncher<Intent>
    private val requestCodeNotificationPolicyAccess = 1

    private lateinit var tvPermissionStatus: TextView
    private lateinit var swListeningEnabled: SwitchCompat
    private lateinit var swOnlyKnownBanks: SwitchCompat
    private lateinit var etWebhookUrl: EditText
    private lateinit var etWebhookSecret: EditText
    private lateinit var etOrderPrefix: EditText
    private lateinit var tvActivityLogs: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        notificationListenerUtil = NotificationListenerUtil(this)

        tvPermissionStatus = findViewById(R.id.tvPermissionStatus)
        swListeningEnabled = findViewById(R.id.swListeningEnabled)
        swOnlyKnownBanks = findViewById(R.id.swOnlyKnownBanks)
        etWebhookUrl = findViewById(R.id.etWebhookUrl)
        etWebhookSecret = findViewById(R.id.etWebhookSecret)
        etOrderPrefix = findViewById(R.id.etOrderPrefix)
        tvActivityLogs = findViewById(R.id.tvActivityLogs)

        notiLisPerLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                val granted = notificationListenerUtil.isNotificationServiceEnabled()
                handleNotificationListenerPermissionResult(granted)
            }

        loadSavedSettings()
        updateStatusAndLogs()
    }

    override fun onResume() {
        super.onResume()
        updateStatusAndLogs()
    }

    private fun loadSavedSettings() {
        etWebhookUrl.setText(AutoPayPrefs.getWebhookUrl(this))
        etWebhookSecret.setText(AutoPayPrefs.getWebhookSecret(this))
        etOrderPrefix.setText(AutoPayPrefs.getOrderPrefix(this))
        swOnlyKnownBanks.isChecked = AutoPayPrefs.isOnlyKnownBankApps(this)
        swListeningEnabled.isChecked = AutoPayPrefs.isListeningEnabled(this)
    }

    private fun updateStatusAndLogs() {
        val enabled = notificationListenerUtil.isNotificationServiceEnabled()
        tvPermissionStatus.text = if (enabled) {
            "Notification Listener: ACTIVE (ready to detect ${AutoPayPrefs.getOrderPrefix(this)}xxxxxx orders)"
        } else {
            "Notification Listener: NOT GRANTED — tap 'Notification Access' below to enable"
        }

        val logs = AutoPayPrefs.getLogs(this)
        tvActivityLogs.text = if (logs.isEmpty()) {
            "No matched payment notifications yet."
        } else {
            logs.joinToString("\n")
        }
    }

    fun saveConfiguration(view: View?) {
        val url = etWebhookUrl.text.toString().trim()
        val secret = etWebhookSecret.text.toString().trim()
        val prefix = etOrderPrefix.text.toString().trim()

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            Toast.makeText(this, "Webhook URL must start with http:// or https://", Toast.LENGTH_LONG).show()
            return
        }

        AutoPayPrefs.saveConfig(
            context = this,
            webhookUrl = url,
            webhookSecret = secret,
            orderPrefix = prefix,
            onlyKnownBankApps = swOnlyKnownBanks.isChecked,
            listeningEnabled = swListeningEnabled.isChecked
        )
        loadSavedSettings()
        updateStatusAndLogs()
        Toast.makeText(this, "Saved Auto Payment settings", Toast.LENGTH_SHORT).show()
    }

    fun sendTestWebhook(view: View?) {
        saveConfiguration(null)
        val url = AutoPayPrefs.getWebhookUrl(this)
        val secret = AutoPayPrefs.getWebhookSecret(this)
        val prefix = AutoPayPrefs.getOrderPrefix(this)
        val sampleCode = "${prefix}000000"
        val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).format(Date())

        val payload = XInfo(
            groupKey = "0|com.VCB|test|${System.currentTimeMillis()}",
            postTime = Date(),
            strPostTime = nowStr,
            title = "Vietcombank (Test)",
            msg = "SD TK 0123456789 +100,000VND luc $nowStr. ND: $sampleCode",
            pck = "com.VCB",
            orderCode = sampleCode,
            amount = 100000L
        )

        Toast.makeText(this, "Sending test webhook to server...", Toast.LENGTH_SHORT).show()
        Thread {
            try {
                val (code, body) = PushingWorker.postJson(url, Gson().toJson(payload), secret)
                val summary = "TEST $sampleCode -> HTTP $code"
                AutoPayPrefs.appendLog(this, summary)
                runOnUiThread {
                    updateStatusAndLogs()
                    Toast.makeText(this, "$summary: ${body.take(100)}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                val msg = "TEST ERROR -> ${e.message ?: "connection failed"}"
                AutoPayPrefs.appendLog(this, msg)
                runOnUiThread {
                    updateStatusAndLogs()
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    fun refreshLogs(view: View?) {
        updateStatusAndLogs()
    }

    fun clearLogs(view: View?) {
        AutoPayPrefs.clearLogs(this)
        updateStatusAndLogs()
    }

    private fun handleNotificationListenerPermissionResult(granted: Boolean) {
        updateStatusAndLogs()
        val msg = if (granted) Constants.PERMISSION_GRANTED else Constants.PERMISSION_DENIED
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    fun settingPermission(view: View?) {
        notificationListenerUtil.requestNotificationListenerPermission(notiLisPerLauncher)
    }

    fun checkAutoStartPermission(view: View) {
        AutoStartHelper.instance.getAutoStartPermission(applicationContext)
    }

    fun notifyOnLockScreenPermission(v: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val pers = arrayOf(
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.ACCESS_NOTIFICATION_POLICY
            )
            ActivityCompat.requestPermissions(this, pers, requestCodeNotificationPolicyAccess)
        } else {
            Toast.makeText(this, "Notifications are enabled by default on Android < 13", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("BatteryLife")
    fun permissionBattery(view: View) {
        val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            startActivity(
                Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName")
                )
            )
        } else {
            Toast.makeText(this, "Battery optimization already exempted", Toast.LENGTH_SHORT).show()
        }
    }
}
