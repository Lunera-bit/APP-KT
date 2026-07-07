package com.CYRYEL.com.data.promotion

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebasePromotionRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : PromotionRepository {

    override suspend fun getPromotionById(id: String): Promotion? {
        val doc = firestore.collection("promotions").document(id).get().await()
        val data = doc.data ?: return null
        return docToPromotion(doc.id, data)
    }

    override fun getActivePromotions(): Flow<List<Promotion>> = callbackFlow {
        val listener = firestore.collection("promotions")
            .whereEqualTo("isActive", true)
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                val promotions = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    docToPromotion(doc.id, data)
                }
                trySend(promotions)
            }

        awaitClose { listener.remove() }
    }

    private fun docToPromotion(id: String, data: Map<String, Any>): Promotion {
        return Promotion(
            id = id,
            name = data["name"] as? String ?: "",
            description = data["description"] as? String ?: "",
            imageUrl = data["imageUrl"] as? String ?: data["foto"] as? String ?: "",
            originalPrice = (data["originalPrice"] as? Number)?.toDouble() ?: 0.0,
            finalPrice = (data["finalPrice"] as? Number)?.toDouble() ?: 0.0,
            discount = (data["discount"] as? Number)?.toDouble() ?: 0.0,
            discountPercent = (data["discountPercent"] as? Number)?.toDouble() ?: 0.0,
            savings = (data["savings"] as? Number)?.toDouble() ?: 0.0,
            products = parsePromotionProducts(data["products"]),
            isActive = data["isActive"] as? Boolean ?: true,
            stockRemaining = (data["stockRemaining"] as? Long)?.toInt() ?: Int.MAX_VALUE,
            points = (data["points"] as? Long)?.toInt() ?: 0,
            expiresAt = (data["expiresAt"] as? com.google.firebase.Timestamp)?.toDate()?.time
        )
    }

    private fun parsePromotionProducts(raw: Any?): List<PromotionProduct> {
        return when (raw) {
            is List<*> -> raw.mapNotNull { item ->
                when (item) {
                    is Map<*, *> -> PromotionProduct(
                        productId = (item["productId"] as? String) ?: "",
                        productName = (item["productName"] as? String) ?: "",
                        quantity = (item["quantity"] as? Number)?.toInt() ?: 1,
                        originalPrice = (item["originalPrice"] as? Number)?.toDouble() ?: 0.0
                    )
                    else -> null
                }
            }
            else -> emptyList()
        }
    }
}
