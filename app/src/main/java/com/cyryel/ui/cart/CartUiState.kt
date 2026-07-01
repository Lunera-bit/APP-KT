package com.cyryel.ui.cart

import com.cyryel.data.cart.CartItem

data class CartUiState(
    val items: List<CartItem> = emptyList()
) {
    val subtotal: Double get() = items.sumOf { it.subtotal }
    val itemCount: Int get() = items.sumOf { it.quantity }
}
