package com.cyryel.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.cyryel.ui.checkout.CheckoutUiState
import com.cyryel.ui.checkout.OrderSnapshot

private const val STORE_PHONE = "51925510147"

fun openWhatsAppWithOrder(context: Context, uiState: CheckoutUiState) {
    val snapshot = uiState.lastOrderSnapshot ?: return
    val message = generateOrderMessage(uiState.orderId, snapshot)
    val encodedMessage = Uri.encode(message)
    val whatsappUrl = "https://wa.me/$STORE_PHONE?text=$encodedMessage"
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse(whatsappUrl)
    }
    context.startActivity(intent)
}

private fun generateOrderMessage(orderId: String, snapshot: OrderSnapshot): String {
    val lines = mutableListOf<String>()

    lines.add("*📦 NUEVO PEDIDO - TIENDA CYRYEL*")
    lines.add("")
    lines.add("*ID Pedido:* $orderId")
    lines.add("*Cliente:* ${snapshot.recipientName}")
    lines.add("*Teléfono:* ${snapshot.phone}")
    lines.add("")

    lines.add("*🛍️ PRODUCTOS:*")
    snapshot.items.forEach { item ->
        var productLine = "• ${item.productName} x${item.quantity}"
        if (!item.variantName.isNullOrBlank()) {
            productLine += " - ${item.variantName}"
        }
        productLine += " = S/ ${"%.2f".format(item.subtotal)}"
        lines.add(productLine)
    }
    lines.add("")

    lines.add("*💰 RESUMEN DE PAGO:*")
    lines.add("Subtotal: S/ ${"%.2f".format(snapshot.subtotal)}")
    if (snapshot.pointsUsed > 0) {
        lines.add("Puntos usados: ${snapshot.pointsUsed} pts")
    }
    if (snapshot.pointsDiscount > 0) {
        lines.add("Descuento (puntos): -S/ ${"%.2f".format(snapshot.pointsDiscount)}")
    }
    lines.add("*Total: S/ ${"%.2f".format(snapshot.subtotal)}*")
    lines.add("")

    lines.add("*📍 DIRECCIÓN DE ENTREGA:*")
    if (snapshot.deliveryMethod == "tienda") {
        lines.add("Recojo en tienda")
    } else {
        lines.add(snapshot.street)
        lines.add(snapshot.city)
        if (snapshot.reference.isNotBlank()) {
            lines.add("Referencia: ${snapshot.reference}")
        }
        if (snapshot.latitude != 0.0 && snapshot.longitude != 0.0) {
            val mapsUrl = "https://maps.google.com/?q=${snapshot.latitude},${snapshot.longitude}"
            lines.add("Ubicación en mapa: $mapsUrl")
        }
    }
    lines.add("")

    val paymentLabel = if (snapshot.paymentMethod == "codigo") "Banca móvil" else "Contra entrega"
    lines.add("*💳 Método de pago:* $paymentLabel")
    if (snapshot.paymentMethod == "codigo") {
        lines.add("")
        lines.add("*🏦 Cuentas para pagar:*")
        lines.add("BBVA: 0011-0264-0200275841")
        lines.add("BCP: 2957127650060")
        lines.add("InterBank: 00229500712765006041")
        lines.add("Yape/Plin: 925510147")
        lines.add("Titular: CYRYEL Eirl")
        lines.add("Monto a pagar: S/ ${"%.2f".format(snapshot.subtotal)}")
        lines.add("Por favor envía la captura del pago por este WhatsApp.")
    }

    if (snapshot.notes.isNotBlank()) {
        lines.add("*Notas:* ${snapshot.notes}")
    }

    lines.add("")
    lines.add("Confirmar recepción y estado del pedido.")

    return lines.joinToString("\n")
}
