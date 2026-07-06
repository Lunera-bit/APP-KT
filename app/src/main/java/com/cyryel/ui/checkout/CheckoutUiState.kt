package com.cyryel.ui.checkout

import com.cyryel.data.cart.CartItem
import com.cyryel.data.user.Address

object StoreCoordinates {
    const val LATITUDE = -11.567832
    const val LONGITUDE = -77.269716
}

enum class CheckoutStep(val step: Int, val title: String) {
    REVIEW(0, "Revisar pedido"),
    DELIVERY(1, "Direccion de entrega"),
    CONTACT(2, "Informacion de contacto"),
    PAYMENT(3, "Metodo de pago"),
    CONFIRM(4, "Confirmar");

    fun previous(): CheckoutStep = entries[(ordinal - 1).coerceAtLeast(0)]
}

data class OrderSnapshot(
    val items: List<CartItem>,
    val subtotal: Double,
    val deliveryMethod: String,
    val street: String,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val reference: String,
    val recipientName: String,
    val phone: String,
    val notes: String,
    val paymentMethod: String,
    val pointsUsed: Int = 0,
    val pointsDiscount: Double = 0.0
)

data class CheckoutUiState(
    val currentStep: CheckoutStep = CheckoutStep.REVIEW,
    val items: List<CartItem> = emptyList(),
    val deliveryMethod: String = "domicilio",
    val street: String = "",
    val city: String = "",
    val latitude: Double = StoreCoordinates.LATITUDE,
    val longitude: Double = StoreCoordinates.LONGITUDE,
    val reference: String = "",
    val recipientName: String = "",
    val phone: String = "",
    val notes: String = "",
    val documentType: String = "dni",
    val documentNumber: String = "",
    val paymentMethod: String = "contra_entrega",
    val fieldErrors: Map<String, String> = emptyMap(),
    val isPlacingOrder: Boolean = false,
    val isLoadingProfile: Boolean = true,
    val orderCreatedMessage: String? = null,
    val orderId: String = "",
    val errorMessage: String? = null,
    val lastOrderSnapshot: OrderSnapshot? = null,
    val savedAddresses: List<Address> = emptyList(),
    val selectedAddressId: String? = null
) {
    val subtotal: Double get() = items.sumOf { it.subtotal }

    val canProceedFromStep0: Boolean get() = items.isNotEmpty()
    val canProceedFromStep1: Boolean get() = deliveryMethod == "tienda" || (street.isNotBlank() && city.isNotBlank())
    val canProceedFromStep2: Boolean get() = recipientName.isNotBlank() && phone.length == 9
    val canProceedFromStep3: Boolean get() = paymentMethod.isNotBlank()
    val canProceedFromStep4: Boolean get() = true
}
