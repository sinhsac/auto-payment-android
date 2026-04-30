package com.hotroso.autopay.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.hotroso.autopay.models.NotificationEntity

@Dao
interface NotificationDao {
    @Insert
    fun insert(entity: NotificationEntity)

    @Query("SELECT * FROM notifications WHERE synced = 0 ORDER BY id ASC LIMIT 100")
    fun getPending(): List<NotificationEntity>

    @Query("UPDATE notifications SET synced = 1 WHERE id IN (:ids)")
    fun markSynced(ids: List<Long>)

    @Query("DELETE FROM notifications WHERE synced = 1")
    fun clearSynced()
}
