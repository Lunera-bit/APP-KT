package com.cyryel.data.promotion

data class Promotion(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val originalPrice: Double = 0.0,
    val finalPrice: Double = 0.0,
    val discount: Double = 0.0,
    val discountPercent: Double = 0.0,
    val savings: Double = 0.0,
    val products: List<PromotionProduct> = emptyList(),
    val isActive: Boolean = true,
    val stockRemaining: Int = Int.MAX_VALUE,
    val points: Int = 0,
    val expiresAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class PromotionProduct(
    val productId: String = "",
    val productName: String = "",
    val quantity: Int = 1,
    val originalPrice: Double = 0.0
)
