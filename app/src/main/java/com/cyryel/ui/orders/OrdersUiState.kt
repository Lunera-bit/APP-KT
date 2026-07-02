package com.cyryel.ui.orders

import com.cyryel.data.order.Order

data class OrdersUiState(
    val isLoading: Boolean = true,
    val allOrders: List<Order> = emptyList(),
    val selectedFilter: String = "todos",
    val startDate: Long? = null,
    val endDate: Long? = null,
    val errorMessage: String? = null,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val lastOrderTimestamp: Long? = null,
    val filteredOrders: List<Order> = emptyList()
) {
    val orders: List<Order> get() = allOrders
}

fun List<Order>.applyFilters(filter: String, startDate: Long?, endDate: Long?): List<Order> {
    val statusFiltered = if (filter == "todos") this else filter { it.status == filter }
    return if (startDate != null || endDate != null) {
        statusFiltered.filter { order ->
            val matchesStart = startDate == null || order.createdAt >= startDate
            val matchesEnd = endDate == null || order.createdAt <= endDate
            matchesStart && matchesEnd
        }
    } else {
        statusFiltered
    }
}
