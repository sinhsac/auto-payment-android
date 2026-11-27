package com.hotroso.autopay.utils

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AutoPayPrefs {
    private const val PREF_NAME = "auto_pay_config"
    private const val KEY_WEBHOOK_URL = "webhook_url"
    private const val KEY_WEBHOOK_SECRET = "webhook_secret"
    private const val KEY_ORDER_PREFIX = "order_prefix"
    private const val KEY_ONLY_KNOWN_BANKS = "only_known_banks"
    private const val KEY_LISTENING_ENABLED = "listening_enabled"
    private const val KEY_RECENT_LOGS = "recent_logs"

    const val DEFAULT_WEBHOOK_URL = "http://192.168.1.100:3000/api/webhooks/bank-notify"
    const val DEFAULT_ORDER_PREFIX = "AP"
    private const val MAX_LOG_LINES = 25

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getWebhookUrl(context: Context): String =
        prefs(context).getString(KEY_WEBHOOK_URL, DEFAULT_WEBHOOK_URL)?.trim() ?: DEFAULT_WEBHOOK_URL

    fun getWebhookSecret(context: Context): String =
        prefs(context).getString(KEY_WEBHOOK_SECRET, "")?.trim() ?: ""

    fun getOrderPrefix(context: Context): String =
        prefs(context).getString(KEY_ORDER_PREFIX, DEFAULT_ORDER_PREFIX)?.trim()
            ?.uppercase(Locale.ROOT)
            ?.ifEmpty { DEFAULT_ORDER_PREFIX } ?: DEFAULT_ORDER_PREFIX

    fun isOnlyKnownBankApps(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ONLY_KNOWN_BANKS, false)

    fun isListeningEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_LISTENING_ENABLED, true)

    fun saveConfig(
        context: Context,
        webhookUrl: String,
        webhookSecret: String,
        orderPrefix: String,
        onlyKnownBankApps: Boolean,
        listeningEnabled: Boolean
    ) {
        val cleanPrefix = orderPrefix.replace(Regex("[^A-Za-z0-9]"), "")
            .uppercase(Locale.ROOT)
            .ifEmpty { DEFAULT_ORDER_PREFIX }

        prefs(context).edit()
            .putString(KEY_WEBHOOK_URL, webhookUrl.trim())
            .putString(KEY_WEBHOOK_SECRET, webhookSecret.trim())
            .putString(KEY_ORDER_PREFIX, cleanPrefix)
            .putBoolean(KEY_ONLY_KNOWN_BANKS, onlyKnownBankApps)
            .putBoolean(KEY_LISTENING_ENABLED, listeningEnabled)
            .apply()
    }

    @Synchronized
    fun appendLog(context: Context, entry: String) {
        val ts = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH).format(Date())
        val existing = getLogs(context).toMutableList()
        existing.add(0, "[$ts] $entry")
        val trimmed = existing.take(MAX_LOG_LINES)
        prefs(context).edit()
            .putString(KEY_RECENT_LOGS, trimmed.joinToString("\n"))
            .apply()
    }

    fun getLogs(context: Context): List<String> {
        val raw = prefs(context).getString(KEY_RECENT_LOGS, "") ?: ""
        return if (raw.isBlank()) emptyList() else raw.split("\n")
    }

    fun clearLogs(context: Context) {
        prefs(context).edit().remove(KEY_RECENT_LOGS).apply()
    }
}
