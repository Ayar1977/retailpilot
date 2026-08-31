package com.salespro.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "sale_line_allocations",
    foreignKeys = [
        ForeignKey(
            entity = SaleLine::class,
            parentColumns = ["id"],
            childColumns = ["saleLineId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = StockBatch::class,
            parentColumns = ["id"],
            childColumns = ["batchId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["saleLineId"]),
        Index(value = ["batchId"])
    ]
)
data class SaleLineAllocation(
    @PrimaryKey val id: String = "ALLOC-${UUID.randomUUID()}",
    val saleLineId: String,
    val batchId: String,
    val quantity: Long,
    val unitCostMinor: Long
)
