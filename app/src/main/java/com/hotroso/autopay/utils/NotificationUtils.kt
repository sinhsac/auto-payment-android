package com.hotroso.autopay.utils

import android.service.notification.StatusBarNotification

object NotificationUtils {
    fun getTitleRaw(sbn: StatusBarNotification): String? {
        val extras = sbn.notification.extras
        return extras.getCharSequence("android.title")?.toString()
            ?: extras.getString("android.title")
    }

    fun getMessage(sbn: StatusBarNotification): String? {
        val extras = sbn.notification.extras
        return extras.getCharSequence("android.text")?.toString()
            ?: extras.getString("android.text")
            ?: ""
    }
}
