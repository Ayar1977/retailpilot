package com.salespro.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "reference_links",
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.SET_NULL // If product is deleted, link stays but productId becomes null so we know it was utilized and is now deleted.
        )
    ],
    indices = [Index(value = ["templateKey"])]
)
data class ReferenceProductLink(
    @PrimaryKey
    val templateKey: String,
    val productId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)
