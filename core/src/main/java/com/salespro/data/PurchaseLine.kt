package com.salespro.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "purchase_lines",
    foreignKeys = [
        ForeignKey(
            entity = Purchase::class,
            parentColumns = ["id"],
            childColumns = ["purchaseId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = StockBatch::class,
            parentColumns = ["id"],
            childColumns = ["batchId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["purchaseId"]),
        Index(value = ["productId"]),
        Index(value = ["batchId"])
    ]
)
data class PurchaseLine(
    @PrimaryKey val id: String = "PL-${UUID.randomUUID()}",
    val purchaseId: String,
    val productId: Long,
    val batchId: String? = null,
    val quantity: Long,
    val unitCostMinor: Long,
    val sellingPriceMinor: Long,
    val discountMinor: Long = 0L,
    val taxMinor: Long = 0L
)
