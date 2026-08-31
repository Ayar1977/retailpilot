package com.salespro.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "stock_movements",
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
        Index(value = ["referenceId"])
    ]
)
data class StockMovement(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val productId: Long,
    val type: StockMovementType,
    val quantityChangeBaseUnits: Long,
    val referenceType: String? = null,
    val referenceId: String? = null,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String? = null
)
