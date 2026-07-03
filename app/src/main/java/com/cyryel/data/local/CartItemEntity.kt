package com.cyryel.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey
    val compositeId: String,
    val productId: String,
    val productName: String,
    val quantity: Int,
    val price: Double,
    val subtotal: Double,
    val productJson: String,
    val variantName: String?
)
