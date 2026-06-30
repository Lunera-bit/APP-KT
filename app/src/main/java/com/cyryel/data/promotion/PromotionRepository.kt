package com.cyryel.data.promotion

interface PromotionRepository {
    suspend fun getActivePromotions(): Result<List<Promotion>>
}
