package com.hotroso.autopay.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.security.MessageDigest

class PaymentDatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    private val appContext = context.applicationContext

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_PAYMENTS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                fingerprint TEXT NOT NULL UNIQUE,
                order_code TEXT NOT NULL,
                amount INTEGER NOT NULL,
                bank_name TEXT NOT NULL,
                package_name TEXT NOT NULL,
                title TEXT NOT NULL,
                raw_message TEXT NOT NULL,
                group_key TEXT NOT NULL,
                post_time_ms INTEGER NOT NULL,
                str_post_time TEXT NOT NULL,
                sync_status TEXT NOT NULL,
                http_code INTEGER,
                server_outcome TEXT,
                last_error TEXT,
                retry_count INTEGER NOT NULL DEFAULT 0,
                created_at_ms INTEGER NOT NULL,
                synced_at_ms INTEGER
            );
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_payments_status ON $TABLE_PAYMENTS(sync_status);")
        db.execSQL("CREATE INDEX idx_payments_order_code ON $TABLE_PAYMENTS(order_code);")
        db.execSQL("CREATE INDEX idx_payments_created ON $TABLE_PAYMENTS(created_at_ms DESC);")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Future migrations go here
    }

    @Synchronized
    fun insertIfNew(record: PaymentRecord): Long {
        val cv = ContentValues().apply {
            put("fingerprint", record.fingerprint)
            put("order_code", record.orderCode)
            put("amount", record.amount)
            put("bank_name", record.bankName)
            put("package_name", record.packageName)
            put("title", record.title)
            put("raw_message", record.rawMessage)
            put("group_key", record.groupKey)
            put("post_time_ms", record.postTimeMs)
            put("str_post_time", record.strPostTime)
            put("sync_status", record.syncStatus.name)
            put("http_code", record.httpCode)
            put("server_outcome", record.serverOutcome)
            put("last_error", record.lastError)
            put("retry_count", record.retryCount)
            put("created_at_ms", record.createdAtMs)
            put("synced_at_ms", record.syncedAtMs)
        }
        val id = writableDatabase.insertWithOnConflict(TABLE_PAYMENTS, null, cv, SQLiteDatabase.CONFLICT_IGNORE)
        if (id > 0L) {
            notifyDataChanged()
        }
        return id
    }

    fun getById(id: Long): PaymentRecord? {
        readableDatabase.query(
            TABLE_PAYMENTS,
            null,
            "id = ?",
            arrayOf(id.toString()),
            null,
            null,
            null,
            "1"
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.toPaymentRecord() else null
        }
    }

    fun getAll(statusFilter: SyncStatus? = null, query: String = "", limit: Int = 200): List<PaymentRecord> {
        val whereParts = mutableListOf<String>()
        val args = mutableListOf<String>()

        if (statusFilter != null) {
            whereParts.add("sync_status = ?")
            args.add(statusFilter.name)
        }
        if (query.isNotBlank()) {
            val like = "%${query.trim()}%"
            whereParts.add("(order_code LIKE ? OR bank_name LIKE ? OR raw_message LIKE ?)")
            args.add(like)
            args.add(like)
            args.add(like)
        }

        val where = if (whereParts.isEmpty()) null else whereParts.joinToString(" AND ")
        val result = mutableListOf<PaymentRecord>()
        readableDatabase.query(
            TABLE_PAYMENTS,
            null,
            where,
            if (args.isEmpty()) null else args.toTypedArray(),
            null,
            null,
            "created_at_ms DESC",
            limit.toString()
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(cursor.toPaymentRecord())
            }
        }
        return result
    }

    fun getPendingOrFailed(limit: Int = 100): List<PaymentRecord> {
        val result = mutableListOf<PaymentRecord>()
        readableDatabase.query(
            TABLE_PAYMENTS,
            null,
            "sync_status IN (?, ?)",
            arrayOf(SyncStatus.PENDING.name, SyncStatus.FAILED.name),
            null,
            null,
            "created_at_ms ASC",
            limit.toString()
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(cursor.toPaymentRecord())
            }
        }
        return result
    }

    fun getStats(): DashboardStats {
        readableDatabase.rawQuery(
            """
            SELECT
                COUNT(*),
                COALESCE(SUM(CASE WHEN sync_status = 'SYNCED' THEN 1 ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN sync_status = 'PENDING' THEN 1 ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN sync_status = 'FAILED' THEN 1 ELSE 0 END), 0),
                COALESCE(SUM(amount), 0)
            FROM $TABLE_PAYMENTS
            """.trimIndent(),
            null
        ).use { c ->
            return if (c.moveToFirst()) {
                DashboardStats(
                    totalCount = c.getInt(0),
                    syncedCount = c.getInt(1),
                    pendingCount = c.getInt(2),
                    failedCount = c.getInt(3),
                    totalAmountVnd = c.getLong(4)
                )
            } else {
                DashboardStats()
            }
        }
    }

    @Synchronized
    fun markSynced(id: Long, httpCode: Int, serverOutcome: String?) {
        val cv = ContentValues().apply {
            put("sync_status", SyncStatus.SYNCED.name)
            put("http_code", httpCode)
            put("server_outcome", serverOutcome ?: "OK")
            putNull("last_error")
            put("synced_at_ms", System.currentTimeMillis())
        }
        writableDatabase.update(TABLE_PAYMENTS, cv, "id = ?", arrayOf(id.toString()))
        notifyDataChanged()
    }

    @Synchronized
    fun markFailed(id: Long, httpCode: Int?, errorMessage: String) {
        writableDatabase.execSQL(
            """
            UPDATE $TABLE_PAYMENTS
            SET sync_status = ?, http_code = ?, last_error = ?, retry_count = retry_count + 1
            WHERE id = ?
            """.trimIndent(),
            arrayOf<Any?>(SyncStatus.FAILED.name, httpCode, errorMessage.take(300), id)
        )
        notifyDataChanged()
    }

    @Synchronized
    fun resetToPending(id: Long) {
        val cv = ContentValues().apply {
            put("sync_status", SyncStatus.PENDING.name)
            putNull("last_error")
        }
        writableDatabase.update(TABLE_PAYMENTS, cv, "id = ?", arrayOf(id.toString()))
        notifyDataChanged()
    }

    @Synchronized
    fun deleteSyncedRecords(): Int {
        val deleted = writableDatabase.delete(TABLE_PAYMENTS, "sync_status = ?", arrayOf(SyncStatus.SYNCED.name))
        if (deleted > 0) {
            notifyDataChanged()
        }
        return deleted
    }

    @Synchronized
    fun deleteById(id: Long) {
        writableDatabase.delete(TABLE_PAYMENTS, "id = ?", arrayOf(id.toString()))
        notifyDataChanged()
    }

    private fun notifyDataChanged() {
        val intent = Intent(ACTION_PAYMENTS_UPDATED).setPackage(appContext.packageName)
        appContext.sendBroadcast(intent)
    }

    private fun Cursor.toPaymentRecord(): PaymentRecord {
        val statusName = getString(getColumnIndexOrThrow("sync_status"))
        val status = runCatching { SyncStatus.valueOf(statusName) }.getOrDefault(SyncStatus.PENDING)
        val httpIdx = getColumnIndexOrThrow("http_code")
        val outcomeIdx = getColumnIndexOrThrow("server_outcome")
        val errIdx = getColumnIndexOrThrow("last_error")
        val syncedIdx = getColumnIndexOrThrow("synced_at_ms")

        return PaymentRecord(
            id = getLong(getColumnIndexOrThrow("id")),
            fingerprint = getString(getColumnIndexOrThrow("fingerprint")),
            orderCode = getString(getColumnIndexOrThrow("order_code")),
            amount = getLong(getColumnIndexOrThrow("amount")),
            bankName = getString(getColumnIndexOrThrow("bank_name")),
            packageName = getString(getColumnIndexOrThrow("package_name")),
            title = getString(getColumnIndexOrThrow("title")),
            rawMessage = getString(getColumnIndexOrThrow("raw_message")),
            groupKey = getString(getColumnIndexOrThrow("group_key")),
            postTimeMs = getLong(getColumnIndexOrThrow("post_time_ms")),
            strPostTime = getString(getColumnIndexOrThrow("str_post_time")),
            syncStatus = status,
            httpCode = if (isNull(httpIdx)) null else getInt(httpIdx),
            serverOutcome = if (isNull(outcomeIdx)) null else getString(outcomeIdx),
            lastError = if (isNull(errIdx)) null else getString(errIdx),
            retryCount = getInt(getColumnIndexOrThrow("retry_count")),
            createdAtMs = getLong(getColumnIndexOrThrow("created_at_ms")),
            syncedAtMs = if (isNull(syncedIdx)) null else getLong(syncedIdx)
        )
    }

    companion object {
        private const val DB_NAME = "auto_payment_history.db"
        private const val DB_VERSION = 1
        private const val TABLE_PAYMENTS = "payment_records"
        const val ACTION_PAYMENTS_UPDATED = "com.hotroso.autopay.PAYMENTS_UPDATED"

        @Volatile
        private var instance: PaymentDatabaseHelper? = null

        fun getInstance(context: Context): PaymentDatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: PaymentDatabaseHelper(context).also { instance = it }
            }
        }

        fun buildFingerprint(packageName: String, strPostTime: String, groupKey: String, rawMessage: String): String {
            val raw = "$packageName|$strPostTime|$groupKey|$rawMessage"
            val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }.take(24)
        }
    }
}
