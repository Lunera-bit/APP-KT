package com.cyryel.data.order

import com.cyryel.data.cart.CartItem

data class CreateOrderRequest(
    val userId: String,
    val userEmail: String,
    val items: List<CartItem>,
    val street: String,
    val city: String,
    val recipientName: String,
    val phone: String,
    val notes: String,
    val shipping: Double = 0.0,
    val deliveryMethod: String = "domicilio",
    val fcmToken: String = ""
)
