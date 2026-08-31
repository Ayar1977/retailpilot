package com.salespro.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "sales",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["invoiceNumber"])
    ]
)
data class Sale(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val invoiceNumber: String,
    val customerId: String? = null,
    val customerPhoneSnapshot: String? = null,
    val subtotalMinor: Long = 0L,
    val discountMinor: Long = 0L,
    val taxMinor: Long = 0L,
    val totalMinor: Long = 0L,
    val paymentStatus: PaymentStatus = PaymentStatus.UNPAID,
    val saleStatus: SaleStatus = SaleStatus.DRAFT,
    val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
    val inventoryApplied: Boolean = false,
    val migrationSource: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val createdBy: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
