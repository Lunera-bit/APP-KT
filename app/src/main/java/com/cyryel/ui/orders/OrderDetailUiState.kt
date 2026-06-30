package com.cyryel.ui.orders

import com.cyryel.data.order.Order

data class OrderDetailUiState(
    val isLoading: Boolean = true,
    val order: Order? = null,
    val errorMessage: String? = null
)
