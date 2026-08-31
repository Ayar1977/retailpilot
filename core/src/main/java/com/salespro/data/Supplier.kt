package com.salespro.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "suppliers")
data class Supplier(
    @PrimaryKey val id: String = "SUP-${UUID.randomUUID()}",
    val name: String,
    val phone: String? = null,
    val address: String? = null,
    val openingBalanceMinor: Long = 0L,
    val active: Boolean = true
)
