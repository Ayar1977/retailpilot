package com.salespro.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A pack (box, carton, tray…) with its own barcode that contains
 * [unitsPerPack] pieces of one product.
 * Example: "Box" barcode 2 = 24 × Cola (barcode 1).
 */
@Entity(
    tableName = "product_packagings",
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["barcode"], unique = true),
        Index(value = ["productId"])
    ]
)
data class ProductPackaging(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val name: String,
    val barcode: String,
    val unitsPerPack: Long,
    val createdAt: Long = System.currentTimeMillis()
)

/** Result of looking up a scanned barcode: a single piece or a whole pack. */
data class BarcodeMatch(
    val product: Product,
    val units: Long,
    val packaging: ProductPackaging?
)
