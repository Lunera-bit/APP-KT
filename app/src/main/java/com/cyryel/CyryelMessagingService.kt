package com.cyryel

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.cyryel.ui.notifications.FcmDebugStore
import com.cyryel.ui.notifications.FcmLogEntry
import com.cyryel.ui.notifications.FcmLogType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CyryelMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token generated: $token")
        FcmDebugStore.log(FcmLogEntry(FcmLogType.TOKEN_GENERATED, "Token generado: ${token.take(20)}..."))

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                FirebaseFirestore.getInstance()
                    .collection("users").document(uid)
                    .set(
                        mapOf("fcmToken" to token, "fcmTokenUpdatedAt" to FieldValue.serverTimestamp()),
                        SetOptions.merge()
                    )
                    .addOnSuccessListener {
                        Log.d("FCM", "Token saved to Firestore for $uid")
                        FcmDebugStore.log(FcmLogEntry(FcmLogType.TOKEN_SAVED, "Token guardado en Firestore para $uid"))
                    }
                    .addOnFailureListener {
                        Log.w("FCM", "Token save failed for $uid", it)
                        FcmDebugStore.log(FcmLogEntry(FcmLogType.TOKEN_SAVE_FAILED, "Error guardando token: ${it.localizedMessage}"))
                    }
            } catch (e: Exception) {
                Log.w("FCM", "Token save failed for $uid", e)
                FcmDebugStore.log(FcmLogEntry(FcmLogType.TOKEN_SAVE_FAILED, "Error guardando token: ${e.localizedMessage}"))
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: message.data["title"] ?: "CYRYEL"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        val from = message.from ?: "unknown"
        val data = message.data.entries.joinToString(", ") { "${it.key}=${it.value}" }

        Log.d("FCM", "Message received - from: $from, title: $title, body: $body, data: $data")
        FcmDebugStore.log(
            FcmLogEntry(
                FcmLogType.MESSAGE_RECEIVED,
                "Mensaje recibido - Titulo: $title, Cuerpo: $body, Data: $data, From: $from"
            )
        )

        if (title.isBlank() && body.isBlank()) {
            FcmDebugStore.log(FcmLogEntry(FcmLogType.MESSAGE_DROPPED, "Mensaje ignorado: title y body vacios"))
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
            FcmDebugStore.log(
                FcmLogEntry(
                    FcmLogType.PERMISSION_CHECK,
                    "POST_NOTIFICATIONS permission: ${if (hasPermission == android.content.pm.PackageManager.PERMISSION_GRANTED) "CONCEDIDO" else "DENEGADO"}"
                )
            )
            if (hasPermission != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                FcmDebugStore.log(FcmLogEntry(FcmLogType.MESSAGE_DROPPED, "Mensaje descartado: permiso de notificacion no concedido"))
                return
            }
        }

        val orderId = message.data["orderId"] ?: ""
        val intent = Intent(this, com.cyryel.MainActivity::class.java)
        if (orderId.isNotBlank()) {
            intent.data = android.net.Uri.parse("cyryel://order/$orderId")
        }
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(com.cyryel.R.drawable.ic_stat_4081896)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
            FcmDebugStore.log(FcmLogEntry(FcmLogType.MESSAGE_SHOWN, "Notificacion mostrada: $title - $body"))
        } catch (e: Exception) {
            Log.e("FCM", "Error showing notification", e)
            FcmDebugStore.log(FcmLogEntry(FcmLogType.ERROR, "Error al mostrar notificacion: ${e.localizedMessage ?: e.javaClass.simpleName}"))
        }
    }

    companion object {
        private const val CHANNEL_ID = "default"
    }
}
