package com.cyryel.data.delivery

import com.cyryel.data.order.Order

data class DeliveryAssignment(
    val id: String = "",
    val orderId: String = "",
    val status: String = "disponible",
    val confirmationCode: String = "",
    val assignedAt: Long = 0L,
    val acceptedAt: Long? = null,
    val startedAt: Long? = null,
    val deliveredAt: Long? = null,
    val deliveryPersonId: String? = null,
    val fcmToken: String? = null,
    val lastLocationLatitude: Double? = null,
    val lastLocationLongitude: Double? = null,
    val lastLocationUpdatedAt: Long? = null
)

interface DeliveryRepository {
    suspend fun getAvailableDeliveries(): Result<List<Pair<DeliveryAssignment, Order>>>
    suspend fun getMyDeliveries(deliveryPersonId: String): Result<List<Pair<DeliveryAssignment, Order>>>
    suspend fun acceptDelivery(
        deliveryId: String,
        orderId: String,
        deliveryPersonId: String,
        deliveryPersonName: String,
        latitude: Double,
        longitude: Double,
        fcmToken: String
    ): Result<Unit>
    suspend fun startDelivery(deliveryId: String, orderId: String): Result<Unit>
    suspend fun completeDelivery(
        deliveryId: String,
        orderId: String,
        confirmationCode: String
    ): Result<Unit>
}
