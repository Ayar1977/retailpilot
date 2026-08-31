package com.salespro.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "sale_lines",
    foreignKeys = [
        ForeignKey(
            entity = Sale::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.NO_ACTION // Maintain record even if parent product terminates
        )
    ],
    indices = [
        Index(value = ["saleId"]),
        Index(value = ["productId"])
    ]
)
data class SaleLine(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val saleId: String,
    val productId: Long? = null,
    val productNameSnapshot: String,
    val barcodeSnapshot: String? = null,
    val quantityBaseUnits: Long,
    val unitPriceMinor: Long,
    val unitCostMinor: Long,
    val discountMinor: Long = 0L,
    val taxMinor: Long = 0L,
    val lineTotalMinor: Long,
    @androidx.room.ColumnInfo(defaultValue = "0")
    val addedAt: Long = System.currentTimeMillis()
)
