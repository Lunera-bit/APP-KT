package com.cyryel.data.cart

import com.cyryel.data.product.Product

data class CartItem(
    val productId: String,
    val productName: String,
    val quantity: Int,
    val price: Double,
    val subtotal: Double,
    val product: Product,
    val variantName: String? = null,
    val redeemedByPoints: Boolean = false,
    val promotionId: String? = null
)
