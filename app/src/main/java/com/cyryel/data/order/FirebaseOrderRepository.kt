package com.cyryel.data.order

import com.cyryel.data.product.ProductVariant
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseOrderRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : OrderRepository {

    override suspend fun createOrder(request: CreateOrderRequest): Result<String> {
        return try {
            val subtotal = request.items.sumOf { it.subtotal }
            val total = subtotal + request.shipping
            val now = Timestamp.now()

            val orderItems = request.items.map { item ->
                val itemMap = mutableMapOf(
                    "productId" to item.productId,
                    "productName" to item.productName,
                    "quantity" to item.quantity,
                    "price" to item.price,
                    "subtotal" to item.subtotal
                )
                if (!item.variantName.isNullOrBlank()) {
                    itemMap["variantName"] = item.variantName
                }
                itemMap
            }

            val payload = hashMapOf<String, Any>(
                "userId" to request.userId,
                "items" to orderItems,
                "subtotal" to subtotal,
                "shipping" to request.shipping,
                "pointsUsed" to 0,
                "pointsDiscount" to 0,
                "total" to total,
                "status" to "pendiente",
                "paymentMethod" to "contra_entrega",
                "paymentStatus" to "completado",
                "deliveryMethod" to request.deliveryMethod,
                "notes" to request.notes,
                "deliveryAddress" to mapOf(
                    "street" to request.street,
                    "city" to request.city,
                    "recipientName" to request.recipientName,
                    "phone" to request.phone,
                    "reference" to "",
                    "documentType" to "dni",
                    "documentNumber" to "",
                    "latitude" to 0.0,
                    "longitude" to 0.0
                ),
                "customerContact" to mapOf(
                    "name" to request.recipientName,
                    "phone" to request.phone
                ),
                "inventoryProcessed" to false,
                "pointsAwarded" to false,
                "pointsAwardAmount" to 0,
                "createdAt" to now,
                "updatedAt" to now
            )

            if (request.userEmail.isNotBlank()) {
                payload["customerEmail"] = request.userEmail
            }
            if (request.fcmToken.isNotBlank()) {
                payload["fcmToken"] = request.fcmToken
            }

            val doc = firestore.collection("orders").add(payload).await()
            Result.success(doc.id)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    override suspend fun getOrdersByUserId(userId: String): Result<List<Order>> {
        return try {
            val snapshot = firestore.collection("orders")
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(30)
                .get()
                .await()
            val orders = snapshot.documents.mapNotNull { orderFromDocument(it) }
            Result.success(orders)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getOrderById(orderId: String): Result<Order> {
        return try {
            val doc = firestore.collection("orders").document(orderId).get().await()
            if (doc.exists()) {
                val order = orderFromDocument(doc)
                    ?: return Result.failure(Exception("Error al parsear orden"))
                Result.success(order)
            } else {
                Result.failure(Exception("Orden no encontrada"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun orderFromDocument(doc: com.google.firebase.firestore.DocumentSnapshot): Order? {
        val data = doc.data ?: return null
        return try {
            val rawItems = data["items"] as? List<*> ?: emptyList<Any>()
            val items = rawItems.mapNotNull { item ->
                when (item) {
                    is Map<*, *> -> {
                        val variantRaw = item["variant"] as? Map<*, *>
                        OrderItem(
                            productId = (item["productId"] as? String) ?: "",
                            productName = (item["productName"] as? String) ?: "",
                            quantity = (item["quantity"] as? Number)?.toInt() ?: 0,
                            price = (item["price"] as? Number)?.toDouble() ?: 0.0,
                            subtotal = (item["subtotal"] as? Number)?.toDouble() ?: 0.0,
                            variant = variantRaw?.let {
                                ProductVariant(
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
            val address = DeliveryAddress(
                street = addrRaw?.get("street") as? String ?: "",
                city = addrRaw?.get("city") as? String ?: "",
                latitude = (addrRaw?.get("latitude") as? Number)?.toDouble() ?: 0.0,
                longitude = (addrRaw?.get("longitude") as? Number)?.toDouble() ?: 0.0,
                recipientName = addrRaw?.get("recipientName") as? String ?: "",
                phone = addrRaw?.get("phone") as? String ?: "",
                reference = addrRaw?.get("reference") as? String ?: "",
                documentType = addrRaw?.get("documentType") as? String ?: "dni",
                documentNumber = addrRaw?.get("documentNumber") as? String ?: ""
            )

            val contactRaw = data["customerContact"] as? Map<*, *>
            val contact = CustomerContact(
                name = contactRaw?.get("name") as? String ?: "",
                phone = contactRaw?.get("phone") as? String ?: ""
            )

            val createdAt = when (val ts = data["createdAt"]) {
                is Timestamp -> ts.toDate().time
                else -> System.currentTimeMillis()
            }
            val updatedAt = when (val ts = data["updatedAt"]) {
                is Timestamp -> ts.toDate().time
                else -> System.currentTimeMillis()
            }

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
                customerContact = contact,
                customerEmail = data["customerEmail"] as? String ?: "",
                fcmToken = data["fcmToken"] as? String ?: "",
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        } catch (e: Exception) {
            null
        }
    }
}
