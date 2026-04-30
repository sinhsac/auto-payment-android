package com.hotroso.autopay

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        notificationListenerUtil = NotificationListenerUtil(this)
        notiLisPerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            updateStatusUI()
        }
        loadServerUrl()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        updateStatusUI()
    }

    private fun loadServerUrl() {
        val prefs = getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
        binding.etServerUrl.setText(prefs.getString(Constants.PREF_SERVER_URL, Constants.DEFAULT_SERVER_URL))
    }

    private fun setupListeners() {
        binding.btnSaveUrl.setOnClickListener {
            val url = binding.etServerUrl.text.toString().trim()
            if (url.isEmpty()) {
                Toast.makeText(this, "Vui long nhap server URL", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
                .edit().putString(Constants.PREF_SERVER_URL, url).apply()
            Toast.makeText(this, "Da luu", Toast.LENGTH_SHORT).show()
        }
        binding.btnNotificationPermission.setOnClickListener {
            notificationListenerUtil.requestNotificationListenerPermission(notiLisPerLauncher)
        }
        binding.btnBatteryPermission.setOnClickListener { openBatterySettings() }
        binding.btnAutoStart.setOnClickListener {
            AutoStartHelper.instance.getAutoStartPermission(applicationContext)
        }
        binding.btnStartService.setOnClickListener {
            startForegroundService(Intent(this, KeepAliveService::class.java))
            Toast.makeText(this, "Service da khoi dong", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateStatusUI() {
        val notiEnabled = notificationListenerUtil.isNotificationServiceEnabled()
        binding.tvStatusNotification.text = if (notiEnabled) "Da cap quyen" else "Chua cap quyen"
        binding.tvStatusNotification.setTextColor(
            getColor(if (notiEnabled) android.R.color.holo_green_dark else android.R.color.holo_red_dark)
        )
        binding.tvStatusBattery.text = "Nhan de cau hinh"
        binding.tvStatusBattery.setTextColor(getColor(android.R.color.darker_gray))
    }

    @SuppressLint("BatteryLife")
    private fun openBatterySettings() {
        startActivity(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
        )
    }
}
