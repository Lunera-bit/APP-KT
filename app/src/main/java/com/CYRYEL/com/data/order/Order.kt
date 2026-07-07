package com.CYRYEL.com.data.order

import com.CYRYEL.com.data.product.ProductVariant

enum class OrderStatus(val value: String) {
    PENDING("pendiente"),
    CONFIRMED("confirmado"),
    IN_DELIVERY("en_reparto"),
    READY_FOR_PICKUP("listo_para_recoger"),
    IN_TRANSIT("en_camino"),
    DELIVERED("entregado"),
    CANCELLED("cancelado"),
    RETURNED("devuelto")
}

enum class PaymentMethod(val value: String) {
    CODIGO("codigo"),
    CONTRA_ENTREGA("contra_entrega")
}

enum class PaymentStatus(val value: String) {
    PENDING("pendiente"),
    COMPLETED("completado"),
    FAILED("fallido"),
    REFUNDED("reembolsado")
}

enum class DeliveryMethod(val value: String) {
    DOMICILIO("domicilio"),
    TIENDA("tienda")
}

data class OrderItem(
    val productId: String,
    val productName: String,
    val quantity: Int,
    val price: Double,
    val subtotal: Double,
    val variant: ProductVariant? = null,
    val redeemedByPoints: Boolean = false,
    val pointsUsed: Int = 0,
    val promotionId: String? = null
)

data class DeliveryAddress(
    val street: String,
    val city: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val recipientName: String = "",
    val phone: String = "",
    val reference: String = "",
    val documentType: String = "dni",
    val documentNumber: String = ""
)

data class CustomerContact(
    val name: String = "",
    val phone: String = ""
)

data class Order(
    val id: String = "",
    val userId: String,
    val items: List<OrderItem>,
    val subtotal: Double,
    val shipping: Double = 0.0,
    val pointsUsed: Int = 0,
    val pointsDiscount: Double = 0.0,
    val pointsEarned: Int = 0,
    val total: Double,
    val status: String = OrderStatus.PENDING.value,
    val paymentMethod: String = PaymentMethod.CONTRA_ENTREGA.value,
    val paymentStatus: String = PaymentStatus.PENDING.value,
    val deliveryMethod: String = DeliveryMethod.DOMICILIO.value,
    val deliveryAddress: DeliveryAddress = DeliveryAddress("", ""),
    val notes: String = "",
    val deliveryNotes: String = "",
    val deliveryConfirmationCode: String = "",
    val deliveryAcceptedAt: Long? = null,
    val deliveryStartedAt: Long? = null,
    val customerContact: CustomerContact = CustomerContact(),
    val customerEmail: String = "",
    val fcmToken: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
