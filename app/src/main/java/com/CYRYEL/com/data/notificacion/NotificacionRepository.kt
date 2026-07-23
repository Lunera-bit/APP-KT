package com.CYRYEL.com.data.notificacion

import kotlinx.coroutines.flow.Flow

interface NotificacionRepository {
    fun getNotificaciones(userId: String): Flow<List<NotificacionData>>
    suspend fun marcarLeida(notificacionId: String)
    suspend fun marcarTodasLeidas(userId: String)
    suspend fun getUnreadCount(userId: String): Result<Int>
}
