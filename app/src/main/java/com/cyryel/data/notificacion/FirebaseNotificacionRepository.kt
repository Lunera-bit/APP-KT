package com.cyryel.data.notificacion

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseNotificacionRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : NotificacionRepository {

    override fun getNotificaciones(userId: String): Flow<List<NotificacionData>> = callbackFlow {
        val query = firestore.collection("notificaciones")
            .whereEqualTo("userId", userId)
            .orderBy("fecha", Query.Direction.DESCENDING)
            .limit(50)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val notis = snapshot?.documents?.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val ts = data["fecha"] as? Timestamp
                NotificacionData(
                    id = doc.id,
                    userId = (data["userId"] as? String) ?: "",
                    titulo = (data["titulo"] as? String) ?: "",
                    mensaje = (data["mensaje"] as? String) ?: "",
                    tipo = (data["tipo"] as? String) ?: "",
                    read = (data["read"] as? Boolean) ?: false,
                    fecha = ts?.toDate()?.time ?: 0L,
                    orderId = data["orderId"] as? String,
                    link = data["link"] as? String,
                    deliveryRelated = (data["deliveryRelated"] as? Boolean) ?: false
                )
            } ?: emptyList()
            trySend(notis)
        }
        awaitClose { listener.remove() }
    }

    override suspend fun marcarLeida(notificacionId: String) {
        try {
            firestore.collection("notificaciones").document(notificacionId)
                .update("read", true).await()
        } catch (_: Exception) { }
    }

    override suspend fun marcarTodasLeidas(userId: String) {
        try {
            val snapshot = firestore.collection("notificaciones")
                .whereEqualTo("userId", userId)
                .whereEqualTo("read", false)
                .get()
                .await()
            val batch = firestore.batch()
            for (doc in snapshot.documents) {
                batch.update(doc.reference, "read", true)
            }
            batch.commit().await()
        } catch (_: Exception) { }
    }

    override suspend fun getUnreadCount(userId: String): Result<Int> {
        return try {
            val snapshot = firestore.collection("notificaciones")
                .whereEqualTo("userId", userId)
                .whereEqualTo("read", false)
                .limit(50)
                .get()
                .await()
            Result.success(snapshot.size())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
