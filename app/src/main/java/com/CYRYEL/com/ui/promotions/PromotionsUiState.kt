package com.CYRYEL.com.ui.promotions

import com.CYRYEL.com.data.promotion.Promotion

data class PromotionsUiState(
    val isLoading: Boolean = true,
    val promotions: List<Promotion> = emptyList(),
    val errorMessage: String? = null
)
