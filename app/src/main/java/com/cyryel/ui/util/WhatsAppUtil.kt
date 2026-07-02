package com.cyryel.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.cyryel.data.cart.CartItem
import com.cyryel.ui.checkout.CheckoutUiState

private const val STORE_PHONE = "51925510147"

fun openWhatsAppWithOrder(context: Context, uiState: CheckoutUiState) {
    val message = generateOrderMessage(uiState)
    val encodedMessage = Uri.encode(message)
    val whatsappUrl = "https://wa.me/$STORE_PHONE?text=$encodedMessage"
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse(whatsappUrl)
    }
    context.startActivity(intent)
}

private fun generateOrderMessage(uiState: CheckoutUiState): String {
    val lines = mutableListOf<String>()

    lines.add("*📦 NUEVO PEDIDO - TIENDA CYRYEL*")
    lines.add("")
    lines.add("*ID Pedido:* ${uiState.orderId}")
    lines.add("*Cliente:* ${uiState.recipientName}")
    lines.add("*Teléfono:* ${uiState.phone}")
    lines.add("")

    lines.add("*🛍️ PRODUCTOS:*")
    uiState.items.forEach { item ->
        var productLine = "• ${item.productName} x${item.quantity}"
        if (!item.variantName.isNullOrBlank()) {
            productLine += " - ${item.variantName}"
        }
        productLine += " = S/ ${"%.2f".format(item.subtotal)}"
        lines.add(productLine)
    }
    lines.add("")

    lines.add("*💰 RESUMEN DE PAGO:*")
    lines.add("Subtotal: S/ ${"%.2f".format(uiState.subtotal)}")
    val shipping = 0.0
    if (shipping > 0) {
        lines.add("Envío: S/ ${"%.2f".format(shipping)}")
    }
    val pointsDiscount = 0.0
    if (pointsDiscount > 0) {
        lines.add("Descuento (puntos): -S/ ${"%.2f".format(pointsDiscount)}")
    }
    lines.add("*Total: S/ ${"%.2f".format(uiState.subtotal)}*")
    lines.add("")

    lines.add("*📍 DIRECCIÓN DE ENTREGA:*")
    if (uiState.deliveryMethod == "tienda") {
        lines.add("Recojo en tienda")
    } else {
        lines.add(uiState.street)
        lines.add(uiState.city)
        if (uiState.reference.isNotBlank()) {
            lines.add("Referencia: ${uiState.reference}")
        }
        if (uiState.latitude != 0.0 && uiState.longitude != 0.0) {
            val mapsUrl = "https://maps.google.com/?q=${uiState.latitude},${uiState.longitude}"
            lines.add("Ubicación en mapa: $mapsUrl")
        }
    }
    lines.add("")

    val paymentLabel = if (uiState.paymentMethod == "codigo") "Banca móvil" else "Contra entrega"
    lines.add("*💳 Método de pago:* $paymentLabel")
    if (uiState.paymentMethod == "codigo") {
        lines.add("")
        lines.add("*🏦 Cuentas para pagar:*")
        lines.add("BBVA: 0011-0264-0200275841")
        lines.add("BCP: 2957127650060")
        lines.add("InterBank: 00229500712765006041")
        lines.add("Yape/Plin: 925510147")
        lines.add("Titular: CYRYEL Eirl")
        lines.add("Monto a pagar: S/ ${"%.2f".format(uiState.subtotal)}")
        lines.add("Por favor envía la captura del pago por este WhatsApp.")
    }

    if (uiState.notes.isNotBlank()) {
        lines.add("*Notas:* ${uiState.notes}")
    }

    lines.add("")
    lines.add("Confirmar recepción y estado del pedido.")

    return lines.joinToString("\n")
}
