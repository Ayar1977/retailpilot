package com.salespro.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["barcode"])
    ]
)
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sku: String = UUID.randomUUID().toString(),
    val barcode: String = "",
    val name: String,
    val retailPriceMinor: Long? = null,
    val activeSellingPriceMinor: Long? = null,
    val pricingUpdatedAt: Long? = null,
    val priceNeedsReview: Boolean = true,
    val costPriceMinor: Long = 0L,
    val measurementType: MeasurementType = MeasurementType.PIECE,
    val measurementScale: Int = 1,
    val imageUri: String = "",
    val lowStockThresholdBaseUnits: Long = 0L,
    val isActive: Boolean = true,
    val brandNameEnglish: String? = null,
    val productTypeEnglish: String? = null,
    val brandNameTelugu: String? = null,
    val productTypeTelugu: String? = null,
    val packageAmountBaseUnits: Long? = null,
    val packageMeasurementType: MeasurementType? = null,
    val teluguEditedByOwner: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
