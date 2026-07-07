package com.CYRYEL.com.data.promotion

import kotlinx.coroutines.flow.Flow

interface PromotionRepository {
    fun getActivePromotions(): Flow<List<Promotion>>
    suspend fun getPromotionById(id: String): Promotion?
}
