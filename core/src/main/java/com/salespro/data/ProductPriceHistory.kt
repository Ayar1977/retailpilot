package com.salespro.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "product_price_history",
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["productId"])
    ]
)
data class ProductPriceHistory(
    @PrimaryKey val id: String = "PH-${UUID.randomUUID()}",
    val productId: Long,
    val previousPriceMinor: Long?,
    val newPriceMinor: Long,
    val sourceBatchId: String? = null,
    val changedAt: Long = System.currentTimeMillis(),
    val reason: String? = null,
    val changedBy: String? = null
)
