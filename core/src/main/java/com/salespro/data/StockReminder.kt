package com.salespro.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "stock_reminders",
    indices = [
        Index(value = ["stockEventId"], unique = true)
    ]
)
data class StockReminder(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val stockEventId: String,
    val productId: Long,
    val reminderType: StockReminderType,
    val scheduledAt: Long? = null, // Absolute Unix millis
    val intendedLocalDate: String? = null,
    val intendedLocalTime: String? = null,
    val intendedZoneId: String? = null,
    val lastNotifiedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val status: StockReminderStatus = StockReminderStatus.PENDING
)
