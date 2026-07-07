package com.CYRYEL.com.data.delivery

data class DeliveryPerson(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val isAvailable: Boolean = true,
    val currentLatitude: Double = 0.0,
    val currentLongitude: Double = 0.0,
    val fcmToken: String = ""
)

data class DeliveryOrder(
    val orderId: String = "",
    val deliveryPersonId: String = "",
    val status: String = "asignado",
    val assignedAt: Long = System.currentTimeMillis(),
    val acceptedAt: Long? = null,
    val deliveredAt: Long? = null,
    val cancelledAt: Long? = null,
    val notes: String = ""
)
