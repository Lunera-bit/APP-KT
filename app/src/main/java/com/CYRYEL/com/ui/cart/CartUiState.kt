package com.CYRYEL.com.ui.cart

import com.CYRYEL.com.data.cart.CartItem

data class CartUiState(
    val items: List<CartItem> = emptyList()
) {
    val subtotal: Double get() = items.sumOf { it.subtotal }
    val itemCount: Int get() = items.sumOf { it.quantity }
}
