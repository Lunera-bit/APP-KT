package com.CYRYEL.com.data.cart

import com.CYRYEL.com.data.product.Product

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
