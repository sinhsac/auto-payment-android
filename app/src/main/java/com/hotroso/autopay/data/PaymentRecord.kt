package com.hotroso.autopay.data

enum class SyncStatus {
    PENDING,
    SYNCED,
    FAILED
}

data class PaymentRecord(
    val id: Long = 0L,
    val fingerprint: String,
    val orderCode: String,
    val amount: Long,
    val bankName: String,
    val packageName: String,
    val title: String,
    val rawMessage: String,
    val groupKey: String,
    val postTimeMs: Long,
    val strPostTime: String,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val httpCode: Int? = null,
    val serverOutcome: String? = null,
    val lastError: String? = null,
    val retryCount: Int = 0,
    val createdAtMs: Long = System.currentTimeMillis(),
    val syncedAtMs: Long? = null
)

data class DashboardStats(
    val totalCount: Int = 0,
    val syncedCount: Int = 0,
    val pendingCount: Int = 0,
    val failedCount: Int = 0,
    val totalAmountVnd: Long = 0L
)
