package com.salespro.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class PurchaseStatus { DRAFT, RECEIVED, PARTIALLY_PAID, PAID, CANCELLED }

@Entity(
    tableName = "purchases",
    foreignKeys = [
        ForeignKey(
            entity = Supplier::class,
            parentColumns = ["id"],
            childColumns = ["supplierId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["supplierId"]),
        Index(value = ["invoiceNumber"])
    ]
)
data class Purchase(
    @PrimaryKey val id: String = "PUR-${UUID.randomUUID()}",
    val supplierId: String? = null,
    val invoiceNumber: String? = null,
    val purchaseDate: Long = System.currentTimeMillis(),
    val subtotalMinor: Long = 0L,
    val discountMinor: Long = 0L,
    val taxMinor: Long = 0L,
    val totalMinor: Long = 0L,
    val paidMinor: Long = 0L,
    val status: PurchaseStatus = PurchaseStatus.DRAFT,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
