package com.CYRYEL.com.data.delivery

import com.CYRYEL.com.data.order.Order
import com.CYRYEL.com.data.order.OrderStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseDeliveryRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : DeliveryRepository {

    override suspend fun getAvailableDeliveries(): Result<List<Pair<DeliveryAssignment, Order>>> {
        return try {
            val snapshot = firestore.collection("deliveries")
                .whereEqualTo("status", "disponible")
                .limit(20)
                .get()
                .await()

            val results = snapshot.documents.mapNotNull { doc ->
                val assignment = deliveryFromDocument(doc) ?: return@mapNotNull null
                val order = fetchOrder(assignment.orderId) ?: return@mapNotNull null
                Pair(assignment, order)
            }
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMyDeliveries(deliveryPersonId: String): Result<List<Pair<DeliveryAssignment, Order>>> {
        return try {
            val snapshot = firestore.collection("deliveries")
                .whereEqualTo("deliveryPersonId", deliveryPersonId)
                .limit(20)
                .get()
                .await()

            val results = snapshot.documents.mapNotNull { doc ->
                val assignment = deliveryFromDocument(doc) ?: return@mapNotNull null
                val order = fetchOrder(assignment.orderId) ?: return@mapNotNull null
                Pair(assignment, order)
            }
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun acceptDelivery(
        deliveryId: String,
        orderId: String,
        deliveryPersonId: String,
        deliveryPersonName: String,
        latitude: Double,
        longitude: Double,
        fcmToken: String
    ): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val deliveryRef = firestore.collection("deliveries").document(deliveryId)
                val deliveryDoc = transaction.get(deliveryRef)
                val currentStatus = deliveryDoc.getString("status") ?: ""

                if (currentStatus != "disponible") {
                    throw IllegalStateException("El pedido ya no esta disponible")
                }

                val now = Timestamp.now()

                transaction.update(deliveryRef, "status", "aceptado")
                transaction.update(deliveryRef, "deliveryPersonId", deliveryPersonId)
                transaction.update(deliveryRef, "fcmToken", fcmToken)
                transaction.update(deliveryRef, "acceptedAt", now)

                val orderRef = firestore.collection("orders").document(orderId)
                transaction.update(orderRef, "status", OrderStatus.IN_DELIVERY.value)
                transaction.update(orderRef, "assignedDeliveryId", deliveryPersonId)
                transaction.update(orderRef, "deliveryPersonName", deliveryPersonName)
                transaction.update(orderRef, "deliveryAcceptedAt", now)
                transaction.update(orderRef, "deliveryPersonLocation", GeoPoint(latitude, longitude))
                transaction.update(orderRef, "updatedAt", now)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun startDelivery(deliveryId: String, orderId: String): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val deliveryRef = firestore.collection("deliveries").document(deliveryId)
                val deliveryDoc = transaction.get(deliveryRef)
                val currentStatus = deliveryDoc.getString("status") ?: ""

                if (currentStatus != "aceptado") {
                    throw IllegalStateException("El pedido debe estar aceptado para iniciar delivery")
                }

                val now = Timestamp.now()
                transaction.update(deliveryRef, "status", "en_camino")
                transaction.update(deliveryRef, "startedAt", now)

                val orderRef = firestore.collection("orders").document(orderId)
                transaction.update(orderRef, "status", OrderStatus.IN_TRANSIT.value)
                transaction.update(orderRef, "deliveryStartedAt", now)
                transaction.update(orderRef, "updatedAt", now)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun completeDelivery(
        deliveryId: String,
        orderId: String,
        confirmationCode: String
    ): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val deliveryRef = firestore.collection("deliveries").document(deliveryId)
                val deliveryDoc = transaction.get(deliveryRef)
                val currentStatus = deliveryDoc.getString("status") ?: ""
                val storedCode = deliveryDoc.getString("confirmationCode") ?: ""

                if (currentStatus != "en_camino") {
                    throw IllegalStateException("El pedido debe estar en camino para completarlo")
                }

                if (storedCode != confirmationCode) {
                    throw IllegalStateException("Codigo de confirmacion incorrecto")
                }

                val now = Timestamp.now()
                transaction.update(deliveryRef, "status", "entregado")
                transaction.update(deliveryRef, "deliveredAt", now)

                val orderRef = firestore.collection("orders").document(orderId)
                transaction.update(orderRef, "status", OrderStatus.DELIVERED.value)
                transaction.update(orderRef, "deliveredAt", now)
                transaction.update(orderRef, "updatedAt", now)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun fetchOrder(orderId: String): Order? {
        return try {
            val doc = firestore.collection("orders").document(orderId).get().await()
            if (!doc.exists()) return null
            orderFromDocument(doc)
        } catch (_: Exception) {
            null
        }
    }

    private fun deliveryFromDocument(doc: com.google.firebase.firestore.DocumentSnapshot): DeliveryAssignment? {
        val data = doc.data ?: return null
        val lastLocation = data["lastLocation"] as? GeoPoint
        return DeliveryAssignment(
            id = doc.id,
            orderId = data["orderId"] as? String ?: return null,
            status = data["status"] as? String ?: "disponible",
            confirmationCode = data["confirmationCode"] as? String ?: "",
            assignedAt = (data["assignedAt"] as? Timestamp)?.toDate()?.time ?: 0L,
            acceptedAt = (data["acceptedAt"] as? Timestamp)?.toDate()?.time,
            startedAt = (data["startedAt"] as? Timestamp)?.toDate()?.time,
            deliveredAt = (data["deliveredAt"] as? Timestamp)?.toDate()?.time,
            deliveryPersonId = data["deliveryPersonId"] as? String,
            fcmToken = data["fcmToken"] as? String,
            lastLocationLatitude = lastLocation?.latitude,
            lastLocationLongitude = lastLocation?.longitude,
            lastLocationUpdatedAt = (data["lastLocationUpdatedAt"] as? Timestamp)?.toDate()?.time
        )
    }

    private fun orderFromDocument(doc: com.google.firebase.firestore.DocumentSnapshot): Order? {
        val data = doc.data ?: return null
        return try {
            com.CYRYEL.com.data.order.FirebaseOrderRepository::class.java
            val rawItems = data["items"] as? List<*> ?: emptyList<Any>()
            val items = rawItems.mapNotNull { item ->
                when (item) {
                    is Map<*, *> -> {
                        val variantRaw = item["variant"] as? Map<*, *>
                        com.CYRYEL.com.data.order.OrderItem(
                            productId = (item["productId"] as? String) ?: "",
                            productName = (item["productName"] as? String) ?: "",
                            quantity = (item["quantity"] as? Number)?.toInt() ?: 0,
                            price = (item["price"] as? Number)?.toDouble() ?: 0.0,
                            subtotal = (item["subtotal"] as? Number)?.toDouble() ?: 0.0,
                            variant = variantRaw?.let {
                                com.CYRYEL.com.data.product.ProductVariant(
                                    nombre = (it["nombre"] as? String) ?: "",
                                    precio = (it["precio"] as? Number)?.toDouble() ?: 0.0,
                                    cantidad = (it["cantidad"] as? Number)?.toInt()
                                )
                            },
                            redeemedByPoints = (item["redeemedByPoints"] as? Boolean) ?: false,
                            pointsUsed = (item["pointsUsed"] as? Number)?.toInt() ?: 0
                        )
                    }
                    else -> null
                }
            }

            val addrRaw = data["deliveryAddress"] as? Map<*, *>
            val address = com.CYRYEL.com.data.order.DeliveryAddress(
                street = addrRaw?.get("street") as? String ?: "",
                city = addrRaw?.get("city") as? String ?: "",
                latitude = (addrRaw?.get("latitude") as? Number)?.toDouble() ?: 0.0,
                longitude = (addrRaw?.get("longitude") as? Number)?.toDouble() ?: 0.0,
                recipientName = addrRaw?.get("recipientName") as? String ?: "",
                phone = addrRaw?.get("phone") as? String ?: "",
                reference = addrRaw?.get("reference") as? String ?: ""
            )

            Order(
                id = doc.id,
                userId = data["userId"] as? String ?: "",
                items = items,
                subtotal = (data["subtotal"] as? Number)?.toDouble() ?: 0.0,
                shipping = (data["shipping"] as? Number)?.toDouble() ?: 0.0,
                pointsUsed = (data["pointsUsed"] as? Number)?.toInt() ?: 0,
                pointsDiscount = (data["pointsDiscount"] as? Number)?.toDouble() ?: 0.0,
                pointsEarned = (data["pointsEarned"] as? Number)?.toInt() ?: 0,
                total = (data["total"] as? Number)?.toDouble() ?: 0.0,
                status = data["status"] as? String ?: "pendiente",
                paymentMethod = data["paymentMethod"] as? String ?: "contra_entrega",
                paymentStatus = data["paymentStatus"] as? String ?: "pendiente",
                deliveryMethod = data["deliveryMethod"] as? String ?: "domicilio",
                deliveryAddress = address,
                notes = data["notes"] as? String ?: "",
                deliveryNotes = data["deliveryNotes"] as? String ?: "",
                deliveryConfirmationCode = data["deliveryConfirmationCode"] as? String ?: "",
                customerContact = com.CYRYEL.com.data.order.CustomerContact(
                    name = (data["customerContact"] as? Map<*, *>)?.get("name") as? String ?: "",
                    phone = (data["customerContact"] as? Map<*, *>)?.get("phone") as? String ?: ""
                ),
                customerEmail = data["customerEmail"] as? String ?: "",
                fcmToken = data["fcmToken"] as? String ?: "",
                createdAt = (data["createdAt"] as? Timestamp)?.toDate()?.time ?: System.currentTimeMillis(),
                updatedAt = (data["updatedAt"] as? Timestamp)?.toDate()?.time ?: System.currentTimeMillis()
            )
        } catch (_: Exception) {
            null
        }
    }
}
