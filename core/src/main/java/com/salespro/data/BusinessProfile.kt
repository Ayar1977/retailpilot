package com.salespro.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "business_profile")
data class BusinessProfile(
    @PrimaryKey val id: Int = 1,
    val businessName: String,
    val ownerName: String?,
    val phoneNumber: String?,
    val address: String?,
    val taxRegistrationNumber: String?,
    val receiptFooter: String?,
    val logoUri: String?,
    val updatedAt: Long = System.currentTimeMillis()
)
