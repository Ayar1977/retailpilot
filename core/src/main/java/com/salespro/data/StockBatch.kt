package com.salespro.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "stock_batches",
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["productId"]),
        Index(value = ["expiryAt"])
    ]
)
data class StockBatch(
    @PrimaryKey val id: String = "BATCH-${UUID.randomUUID()}",
    val productId: Long,
    val batchNumber: String? = null,
    val receivedAt: Long = System.currentTimeMillis(),
    val expiryAt: Long? = null,
    val quantityReceived: Long,
    val quantityRemaining: Long,
    val unitCostMinor: Long,
    val intakeSellingPriceMinor: Long,
    val sourceReference: String? = null,
    @ColumnInfo(defaultValue = "ACTIVE")
    val status: String = "ACTIVE"
)
