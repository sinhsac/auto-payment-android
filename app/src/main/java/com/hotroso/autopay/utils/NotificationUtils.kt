package com.hotroso.autopay.utils

import android.app.Notification
import android.service.notification.StatusBarNotification

object NotificationUtils {
    private const val MAX_OLD_NOTIFICATION_CAN_BE_REPLIED_TIME_MS = 2 * 60 * 1000

    fun isNewNotification(sbn: StatusBarNotification): Boolean {
        return sbn.notification.`when` == 0L ||
            System.currentTimeMillis() - sbn.notification.`when` < MAX_OLD_NOTIFICATION_CAN_BE_REPLIED_TIME_MS
    }

    fun getTitleRaw(sbn: StatusBarNotification): String {
        val extras = sbn.notification.extras ?: return ""
        val bigTitle = extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()
        if (!bigTitle.isNullOrBlank()) {
            return bigTitle.trim()
        }
        return extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: ""
    }

    /**
     * Reads full notification content with priority for EXTRA_BIG_TEXT (used by most
     * Vietnamese banking apps to hold the full balance alert and transfer description),
     * falling back to EXTRA_TEXT_LINES (InboxStyle) and EXTRA_TEXT.
     */
    fun getMessage(sbn: StatusBarNotification): String {
        val extras = sbn.notification.extras ?: return ""

        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()
        if (!bigText.isNullOrEmpty() && isValidMessage(bigText)) {
            return bigText
        }

        val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        if (!textLines.isNullOrEmpty()) {
            val joined = textLines.mapNotNull { it?.toString()?.trim() }
                .filter { it.isNotEmpty() }
                .joinToString(" ")
            if (joined.isNotEmpty() && isValidMessage(joined)) {
                return joined
            }
        }

        val regularText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
        if (!regularText.isNullOrEmpty() && isValidMessage(regularText)) {
            return regularText
        }

        return ""
    }

    private fun isValidMessage(msg: String?): Boolean {
        if (msg.isNullOrBlank() || msg.length <= 2) return false
        val ignored = arrayOf(
            "This message was deleted",
            "new messages",
            "\uD83D\uDCF7 Photo",
            "Calling…",
            "Ringing…",
            "Missed voice call",
            "Incoming voice call",
            "Ongoing video call",
            "Sticker",
            "missed calls",
            "\uD83D\uDCF9 Incoming video call",
            "GIF"
        )
        return ignored.none { msg.contains(it) }
    }
}
