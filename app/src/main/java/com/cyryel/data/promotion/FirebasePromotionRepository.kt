package com.cyryel.data.promotion

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebasePromotionRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : PromotionRepository {

    override suspend fun getActivePromotions(): Result<List<Promotion>> {
        return try {
            val snapshot = firestore.collection("promotions")
                .whereEqualTo("isActive", true)
                .limit(20)
                .get()
                .await()
            val promotions = snapshot.documents.map { doc ->
                val data = doc.data ?: return@map null
                Promotion(
                    id = doc.id,
                    name = data["name"] as? String ?: "",
                    description = data["description"] as? String ?: "",
                    imageUrl = data["imageUrl"] as? String ?: data["foto"] as? String ?: "",
                    originalPrice = (data["originalPrice"] as? Number)?.toDouble() ?: 0.0,
                    finalPrice = (data["finalPrice"] as? Number)?.toDouble() ?: 0.0,
                    discount = (data["discount"] as? Number)?.toDouble() ?: 0.0,
                    savings = (data["savings"] as? Number)?.toDouble() ?: 0.0,
                    products = parsePromotionProducts(data["products"]),
                    isActive = data["isActive"] as? Boolean ?: true,
                    stockRemaining = (data["stockRemaining"] as? Long)?.toInt() ?: Int.MAX_VALUE
                )
            }.filterNotNull()
            Result.success(promotions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parsePromotionProducts(raw: Any?): List<PromotionProduct> {
        return when (raw) {
            is List<*> -> raw.mapNotNull { item ->
                when (item) {
                    is Map<*, *> -> PromotionProduct(
                        productId = (item["productId"] as? String) ?: "",
                        productName = (item["productName"] as? String) ?: "",
                        quantity = (item["quantity"] as? Number)?.toInt() ?: 1
                    )
                    else -> null
                }
            }
            else -> emptyList()
        }
    }
}
