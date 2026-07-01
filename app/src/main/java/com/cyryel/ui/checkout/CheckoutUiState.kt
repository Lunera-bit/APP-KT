package com.cyryel.ui.checkout

import com.cyryel.data.cart.CartItem

enum class CheckoutStep(val step: Int, val title: String) {
    REVIEW(0, "Revisar pedido"),
    DELIVERY(1, "Direccion de entrega"),
    CONTACT(2, "Informacion de contacto"),
    PAYMENT(3, "Metodo de pago"),
    CONFIRM(4, "Confirmar")
}

data class CheckoutUiState(
    val currentStep: CheckoutStep = CheckoutStep.REVIEW,
    val items: List<CartItem> = emptyList(),
    val deliveryMethod: String = "domicilio",
    val street: String = "",
    val city: String = "",
    val reference: String = "",
    val recipientName: String = "",
    val phone: String = "",
    val notes: String = "",
    val paymentMethod: String = "contra_entrega",
    val fieldErrors: Map<String, String> = emptyMap(),
    val isPlacingOrder: Boolean = false,
    val orderCreatedMessage: String? = null,
    val orderId: String = "",
    val errorMessage: String? = null
) {
    val subtotal: Double get() = items.sumOf { it.subtotal }

    val canProceedFromStep0: Boolean get() = items.isNotEmpty()
    val canProceedFromStep1: Boolean get() = deliveryMethod == "tienda" || (street.isNotBlank() && city.isNotBlank())
    val canProceedFromStep2: Boolean get() = recipientName.isNotBlank() && phone.length == 9
    val canProceedFromStep3: Boolean get() = paymentMethod.isNotBlank()
    val canProceedFromStep4: Boolean get() = true
}
