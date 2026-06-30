package com.cyryel.ui.cart

import com.cyryel.data.cart.CartItem

data class CartUiState(
    val items: List<CartItem> = emptyList(),
    val street: String = "",
    val city: String = "",
    val recipientName: String = "",
    val phone: String = "",
    val notes: String = "",
    val deliveryMethod: String = "domicilio",
    val fieldErrors: Map<String, String> = emptyMap(),
    val isPlacingOrder: Boolean = false,
    val orderCreatedMessage: String? = null,
    val errorMessage: String? = null
) {
    val subtotal: Double get() = items.sumOf { it.subtotal }
}
