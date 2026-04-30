package com.hotroso.autopay

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.hotroso.autopay.common.Constants
import com.hotroso.autopay.databinding.ActivitySettingBinding
import com.hotroso.autopay.helpers.AutoStartHelper
import com.hotroso.autopay.listeners.KeepAliveService
import com.hotroso.autopay.utils.NotificationListenerUtil

class SettingActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingBinding
    private lateinit var notificationListenerUtil: NotificationListenerUtil
    private lateinit var notiLisPerLauncher: ActivityResultLauncher<Intent>
    private lateinit var batteryLauncher: ActivityResultLauncher<Intent>

    private val authTypes = listOf("No Auth", "Basic Auth", "X-Auth-Secret", "X-Auth-Token (JWT)")
    private val authKeys  = listOf("none", "basic", "secret", "token")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        notificationListenerUtil = NotificationListenerUtil(this)

        notiLisPerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { updateStatusUI() }
        batteryLauncher    = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { updateStatusUI() }

        setupAuthDropdown()
        loadConfig()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        notificationListenerUtil.rebindIfNeeded()
        updateStatusUI()
    }

    private fun setupListeners() {
        binding.btnNotificationPermission.setOnClickListener {
            notificationListenerUtil.requestNotificationListenerPermission(notiLisPerLauncher)
        }

        binding.btnBatteryPermission.setOnClickListener { openBatterySettings() }

        binding.btnAutoStart.setOnClickListener {
            AutoStartHelper.instance.getAutoStartPermission(applicationContext)
        }

        binding.btnService.setOnClickListener {
            if (isServiceRunning()) {
                startService(Intent(this, KeepAliveService::class.java).apply {
                    action = KeepAliveService.ACTION_STOP
                })
            } else {
                startForegroundService(Intent(this, KeepAliveService::class.java))
            }
            Handler(mainLooper).postDelayed({ updateStatusUI() }, 500)
        }

        binding.btnSaveUrl.setOnClickListener { saveConfig() }
    }

    private fun setupAuthDropdown() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, authTypes)
        binding.spinnerAuthType.setAdapter(adapter)
        binding.spinnerAuthType.setOnItemClickListener { _, _, pos, _ -> showAuthFields(authKeys[pos]) }
    }

    private fun showAuthFields(authType: String) {
        binding.layoutBasicAuth.visibility = if (authType == "basic")  View.VISIBLE else View.GONE
        binding.layoutSecret.visibility    = if (authType == "secret") View.VISIBLE else View.GONE
        binding.layoutToken.visibility     = if (authType == "token")  View.VISIBLE else View.GONE
    }

    private fun loadConfig() {
        val prefs = getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
        binding.etServerUrl.setText(prefs.getString(Constants.PREF_SERVER_URL, Constants.DEFAULT_SERVER_URL))
        val authType  = prefs.getString(Constants.PREF_AUTH_TYPE, Constants.DEFAULT_AUTH_TYPE) ?: Constants.DEFAULT_AUTH_TYPE
        val authIndex = authKeys.indexOf(authType).takeIf { it >= 0 } ?: 0
        binding.spinnerAuthType.setText(authTypes[authIndex], false)
        showAuthFields(authType)
        binding.etAuthUser.setText(prefs.getString(Constants.PREF_AUTH_USER, ""))
        binding.etAuthPass.setText(prefs.getString(Constants.PREF_AUTH_PASS, ""))
        binding.etAuthSecret.setText(prefs.getString(Constants.PREF_AUTH_SECRET, Constants.DEFAULT_AUTH_SECRET))
        binding.etAuthToken.setText(prefs.getString(Constants.PREF_AUTH_TOKEN, ""))
    }

    private fun saveConfig() {
        val url = binding.etServerUrl.text.toString().trim()
        if (url.isEmpty()) {
            Toast.makeText(this, "Vui long nhap server URL", Toast.LENGTH_SHORT).show()
            return
        }
        val authType = authKeys[authTypes.indexOf(binding.spinnerAuthType.text.toString()).takeIf { it >= 0 } ?: 0]
        getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE).edit().apply {
            putString(Constants.PREF_SERVER_URL,   url)
            putString(Constants.PREF_AUTH_TYPE,    authType)
            putString(Constants.PREF_AUTH_USER,    binding.etAuthUser.text.toString())
            putString(Constants.PREF_AUTH_PASS,    binding.etAuthPass.text.toString())
            putString(Constants.PREF_AUTH_SECRET,  binding.etAuthSecret.text.toString())
            putString(Constants.PREF_AUTH_TOKEN,   binding.etAuthToken.text.toString())
            apply()
        }
        Toast.makeText(this, "Da luu", Toast.LENGTH_SHORT).show()
    }

    private fun updateStatusUI() {
        val notiEnabled = notificationListenerUtil.isNotificationServiceEnabled()
        binding.tvStatusNotification.text = if (notiEnabled) "Da cap quyen" else "Chua cap quyen"
        binding.tvStatusNotification.setTextColor(getColor(if (notiEnabled) android.R.color.holo_green_dark else android.R.color.holo_red_dark))

        val pm = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        val batteryOk = pm.isIgnoringBatteryOptimizations(packageName)
        binding.tvStatusBattery.text = if (batteryOk) "Da bo qua toi uu pin" else "Chua cau hinh"
        binding.tvStatusBattery.setTextColor(getColor(if (batteryOk) android.R.color.holo_green_dark else android.R.color.holo_red_dark))

        val running = isServiceRunning()
        binding.tvStatusService.text = if (running) "Dang chay" else "Chua chay"
        binding.tvStatusService.setTextColor(getColor(if (running) android.R.color.holo_green_dark else android.R.color.holo_red_dark))
        binding.btnService.text = if (running) "Dung" else "Khoi dong"
    }

    @SuppressLint("BatteryLife")
    private fun openBatterySettings() {
        val pm = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            batteryLauncher.launch(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
            )
        } else {
            Toast.makeText(this, "Da bo qua toi uu pin roi", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isServiceRunning(): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Int.MAX_VALUE)
            .any { it.service.className == KeepAliveService::class.java.name }
    }
}
