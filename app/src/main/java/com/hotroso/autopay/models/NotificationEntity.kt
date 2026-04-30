package com.hotroso.autopay.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pck: String?,
    val title: String?,
    val msg: String?,
    val groupKey: String?,
    val postTime: String?,
    val synced: Boolean = false
)
