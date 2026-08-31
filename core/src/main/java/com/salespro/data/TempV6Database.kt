package com.salespro.data

import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.RoomDatabase

@Database(entities = [LegacyProduct::class, LegacySale::class], version = 6, exportSchema = true)
abstract class TempV6Database : RoomDatabase()

@Entity(tableName = "products")
data class LegacyProduct(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val costPrice: Double,
    val imageName: String = "",
    val ean: String = "",
    val stockQuantity: Int = 0
)

@Entity(tableName = "sales")
data class LegacySale(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productName: String,
    val finalPrice: Double,
    val profit: Double,
    val quantity: Int = 1,
    val customerPhone: String = "",
    val timestamp: Long = 0L
)
