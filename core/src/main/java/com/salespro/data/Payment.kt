package com.salespro.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = Sale::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["saleId"])
    ]
)
data class Payment(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val saleId: String,
    val method: PaymentMethod,
    val amountMinor: Long,
    val reference: String? = null,
    val status: String,
    val receivedAt: Long = System.currentTimeMillis()
)
