package com.salespro.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "stock_alert_events",
    indices = [
        Index(value = ["productId", "status"]),
        Index(value = ["status"])
    ]
)
data class StockAlertEvent(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val productId: Long,
    val triggerType: StockAlertTriggerType,
    val thresholdSnapshotBaseUnits: Long,
    val stockAtOpenBaseUnits: Long,
    val openedAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null,
    val status: StockAlertStatus = StockAlertStatus.ACTIVE,
    val eventSequence: Long = System.currentTimeMillis()
)
