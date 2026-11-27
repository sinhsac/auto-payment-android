package com.hotroso.autopay.models

import android.service.notification.StatusBarNotification
import com.hotroso.autopay.utils.NotificationUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class XInfo(
    var groupKey: String? = null,
    var postTime: Date? = null,
    var strPostTime: String? = null,
    var title: String? = null,
    var msg: String? = null,
    var pck: String? = null,
    var orderCode: String? = null,
    var amount: Long? = null
) {
    constructor(sbn: StatusBarNotification, extractedOrderCode: String? = null, extractedAmount: Long? = null) : this() {
        val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
        postTime = Date(sbn.postTime)
        strPostTime = postTime?.let { df.format(it) }
        title = NotificationUtils.getTitleRaw(sbn)
        msg = NotificationUtils.getMessage(sbn)
        groupKey = sbn.groupKey
        pck = sbn.packageName
        orderCode = extractedOrderCode
        amount = extractedAmount
    }
}
