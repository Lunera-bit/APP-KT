package com.cyryel.ui.orders

import com.cyryel.data.order.Order

data class OrdersUiState(
    val isLoading: Boolean = true,
    val orders: List<Order> = emptyList(),
    val selectedFilter: String = "todos",
    val errorMessage: String? = null
)
