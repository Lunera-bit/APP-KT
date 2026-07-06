package com.cyryel.data.notificacion

data class NotificacionData(
    val id: String = "",
    val userId: String = "",
    val titulo: String = "",
    val mensaje: String = "",
    val tipo: String = "",
    val read: Boolean = false,
    val fecha: Long = 0L,
    val orderId: String? = null,
    val link: String? = null,
    val deliveryRelated: Boolean = false
)
