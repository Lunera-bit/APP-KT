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
    val reference: String = "",
    val documentType: String = "dni",
    val documentNumber: String = "",
    val shipping: Double = 0.0,
    val deliveryMethod: String = "domicilio",
    val paymentMethod: String = "contra_entrega",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val fcmToken: String = "",
    val pointsUsed: Int = 0,
    val pointsDiscount: Double = 0.0
)
