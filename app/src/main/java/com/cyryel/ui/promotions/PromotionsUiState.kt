package com.cyryel.ui.promotions

import com.cyryel.data.promotion.Promotion

data class PromotionsUiState(
    val isLoading: Boolean = true,
    val promotions: List<Promotion> = emptyList(),
    val errorMessage: String? = null
)
