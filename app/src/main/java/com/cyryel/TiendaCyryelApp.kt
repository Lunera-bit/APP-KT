package com.cyryel

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TiendaCyryelApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val pedidosChannel = NotificationChannel(
                "pedidos_channel",
                "Pedidos",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de pedidos y promociones"
            }
            manager.createNotificationChannel(pedidosChannel)
            val defaultChannel = NotificationChannel(
                "default",
                "Notificaciones",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones generales"
            }
            manager.createNotificationChannel(defaultChannel)
            val locationChannel = NotificationChannel(
                "location_service",
                "Ubicacion en tiempo real",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificacion persistente cuando el repartidor comparte ubicacion"
            }
            manager.createNotificationChannel(locationChannel)
        }
    }
}
