package com.cyryel.ui.orders

import com.cyryel.data.order.Order

data class PromotionBrief(
    val name: String,
    val productQuantities: Map<String, Int> = emptyMap()
)

data class OrderDetailUiState(
    val isLoading: Boolean = true,
    val order: Order? = null,
    val errorMessage: String? = null,
    val isCancelling: Boolean = false,
    val cancelSuccess: Boolean = false,
    val deliveryLatitude: Double? = null,
    val deliveryLongitude: Double? = null,
    val promotionData: Map<String, PromotionBrief> = emptyMap()
)
