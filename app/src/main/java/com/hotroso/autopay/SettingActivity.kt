package com.hotroso.autopay

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.hotroso.autopay.common.Constants
import com.hotroso.autopay.data.PaymentDatabaseHelper
import com.hotroso.autopay.data.PaymentRecord
import com.hotroso.autopay.data.SyncStatus
import com.hotroso.autopay.helpers.AutoStartHelper
import com.hotroso.autopay.ui.AppUiState
import com.hotroso.autopay.ui.AutoPaymentApp
import com.hotroso.autopay.utils.AutoPayPrefs
import com.hotroso.autopay.utils.NotificationListenerUtil
import com.hotroso.autopay.workers.PushingWorker
import com.hotroso.autopay.workers.SyncPendingWorker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingActivity : AppCompatActivity() {

    private lateinit var notificationListenerUtil: NotificationListenerUtil
    private lateinit var notiLisPerLauncher: ActivityResultLauncher<Intent>
    private lateinit var db: PaymentDatabaseHelper

    private val uiState = mutableStateOf(AppUiState())
    private val snackbarHostState = SnackbarHostState()
    private val requestCodeNotificationPolicyAccess = 1

    private val paymentsUpdatedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            reloadDashboard()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        notificationListenerUtil = NotificationListenerUtil(this)
        db = PaymentDatabaseHelper.getInstance(this)
        SyncPendingWorker.schedulePeriodicSync(this)

        notiLisPerLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                val granted = notificationListenerUtil.isNotificationServiceEnabled()
                reloadDashboard()
                val msg = if (granted) Constants.PERMISSION_GRANTED else Constants.PERMISSION_DENIED
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }

        reloadDashboard()

        setContent {
            AutoPaymentApp(
                state = uiState.value,
                snackbarHostState = snackbarHostState,
                onFilterChange = { filter ->
                    uiState.value = uiState.value.copy(selectedFilter = filter)
                    reloadDashboard()
                },
                onSearchChange = { query ->
                    uiState.value = uiState.value.copy(searchQuery = query)
                    reloadDashboard()
                },
                onRefresh = {
                    reloadDashboard()
                    Toast.makeText(this, "Đã làm mới danh sách đơn", Toast.LENGTH_SHORT).show()
                },
                onSyncAllPending = { triggerSyncAll() },
                onRetrySingle = { record -> retrySingleRecord(record) },
                onDeleteRecord = { record ->
                    db.deleteById(record.id)
                    reloadDashboard()
                },
                onSaveConfig = { url, secret, prefix, onlyKnownBanks, listeningEnabled ->
                    saveConfiguration(url, secret, prefix, onlyKnownBanks, listeningEnabled)
                },
                onTestWebhook = { url, secret, prefix ->
                    sendTestWebhook(url, secret, prefix)
                },
                onClearSynced = {
                    val count = db.deleteSyncedRecords()
                    reloadDashboard()
                    Toast.makeText(this, "Đã dọn $count đơn đã đồng bộ", Toast.LENGTH_SHORT).show()
                },
                onRequestNotificationPermission = {
                    notificationListenerUtil.requestNotificationListenerPermission(notiLisPerLauncher)
                },
                onRequestBatteryExemption = { requestBatteryOptimizationExemption() },
                onRequestAutoStart = { AutoStartHelper.instance.getAutoStartPermission(applicationContext) },
                onRequestLockScreenNotifications = { requestAndroid13NotificationPermission() }
            )
        }
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(
            this,
            paymentsUpdatedReceiver,
            IntentFilter(PaymentDatabaseHelper.ACTION_PAYMENTS_UPDATED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(paymentsUpdatedReceiver)
        } catch (_: IllegalArgumentException) {
            // already unregistered
        }
    }

    override fun onResume() {
        super.onResume()
        reloadDashboard()
    }

    private fun reloadDashboard() {
        val current = uiState.value
        val records = db.getAll(current.selectedFilter, current.searchQuery)
        val stats = db.getStats()
        val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        val batteryExempt = powerManager.isIgnoringBatteryOptimizations(packageName)

        uiState.value = current.copy(
            records = records,
            stats = stats,
            isListenerGranted = notificationListenerUtil.isNotificationServiceEnabled(),
            isBatteryExempt = batteryExempt,
            webhookUrl = AutoPayPrefs.getWebhookUrl(this),
            webhookSecret = AutoPayPrefs.getWebhookSecret(this),
            orderPrefix = AutoPayPrefs.getOrderPrefix(this),
            onlyKnownBanks = AutoPayPrefs.isOnlyKnownBankApps(this),
            listeningEnabled = AutoPayPrefs.isListeningEnabled(this)
        )
    }

    private fun saveConfiguration(
        url: String,
        secret: String,
        prefix: String,
        onlyKnownBanks: Boolean,
        listeningEnabled: Boolean
    ) {
        val trimmedUrl = url.trim()
        if (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://")) {
            Toast.makeText(this, "Webhook URL phải bắt đầu bằng http:// hoặc https://", Toast.LENGTH_LONG).show()
            return
        }

        AutoPayPrefs.saveConfig(
            context = this,
            webhookUrl = trimmedUrl,
            webhookSecret = secret,
            orderPrefix = prefix,
            onlyKnownBankApps = onlyKnownBanks,
            listeningEnabled = listeningEnabled
        )
        reloadDashboard()
        Toast.makeText(this, "Đã lưu cấu hình Auto Payment", Toast.LENGTH_SHORT).show()
    }

    private fun triggerSyncAll() {
        val pendingCount = db.getPendingOrFailed().size
        if (pendingCount == 0) {
            Toast.makeText(this, "Tất cả đơn đều đã đồng bộ!", Toast.LENGTH_SHORT).show()
            return
        }
        SyncPendingWorker.enqueueNow(this)
        Toast.makeText(this, "Đang đồng bộ $pendingCount đơn chưa gửi...", Toast.LENGTH_SHORT).show()
    }

    private fun retrySingleRecord(record: PaymentRecord) {
        db.resetToPending(record.id)
        PushingWorker.enqueueForPayment(this, record.id, record.orderCode)
        reloadDashboard()
        Toast.makeText(this, "Đang gửi lại đơn #${record.orderCode}...", Toast.LENGTH_SHORT).show()
    }

    private fun sendTestWebhook(url: String, secret: String, prefix: String) {
        val cleanPrefix = prefix.replace(Regex("[^A-Za-z0-9]"), "").uppercase(Locale.ROOT).ifEmpty { "AP" }
        val sampleCode = "${cleanPrefix}000000"
        val nowMs = System.currentTimeMillis()
        val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).format(Date(nowMs))
        val msg = "SD TK 0123456789 +100,000VND luc $nowStr. ND: $sampleCode"
        val fp = PaymentDatabaseHelper.buildFingerprint("com.VCB", nowStr, "test_$nowMs", msg)

        val recordId = db.insertIfNew(
            PaymentRecord(
                fingerprint = fp,
                orderCode = sampleCode,
                amount = 100000L,
                bankName = "Vietcombank (Test)",
                packageName = "com.VCB",
                title = "Vietcombank (Test)",
                rawMessage = msg,
                groupKey = "test_$nowMs",
                postTimeMs = nowMs,
                strPostTime = nowStr,
                syncStatus = SyncStatus.PENDING
            )
        )

        if (recordId > 0L) {
            PushingWorker.enqueueForPayment(this, recordId, sampleCode)
            reloadDashboard()
            Toast.makeText(this, "Đã tạo giao dịch thử #$sampleCode và gửi lên server...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestAndroid13NotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val pers = arrayOf(
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.ACCESS_NOTIFICATION_POLICY
            )
            ActivityCompat.requestPermissions(this, pers, requestCodeNotificationPolicyAccess)
        } else {
            Toast.makeText(this, "Thông báo đã bật mặc định trên Android < 13", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("BatteryLife")
    private fun requestBatteryOptimizationExemption() {
        val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            startActivity(
                Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName")
                )
            )
        } else {
            Toast.makeText(this, "Đã tắt tối ưu pin cho Auto Payment", Toast.LENGTH_SHORT).show()
        }
    }
}
