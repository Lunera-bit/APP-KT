package com.cyryel.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyryel.data.auth.AuthRepository
import com.cyryel.data.order.Order
import com.cyryel.data.order.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrdersUiState())
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()

    init {
        loadOrders()
    }

    fun loadOrders() {
        val userId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, hasMore = true) }
            val result = orderRepository.getOrdersByUserIdPaginated(userId, null, 5)
            if (result.isSuccess) {
                val (orders, hasMore) = result.getOrDefault(Pair(emptyList(), false))
                val lastTs = orders.lastOrNull()?.createdAt
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        allOrders = orders,
                        hasMore = hasMore,
                        lastOrderTimestamp = lastTs,
                        filteredOrders = orders.applyFilters(state.selectedFilter, state.startDate, state.endDate)
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Error al cargar pedidos"
                    )
                }
            }
        }
    }

    fun loadMoreOrders() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore) return
        val userId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            val result = orderRepository.getOrdersByUserIdPaginated(
                userId, state.lastOrderTimestamp, 5
            )
            if (result.isSuccess) {
                val (newOrders, hasMore) = result.getOrDefault(Pair(emptyList(), false))
                val lastTs = newOrders.lastOrNull()?.createdAt
                _uiState.update { st ->
                    val updated = st.allOrders + newOrders
                    st.copy(
                        isLoadingMore = false,
                        allOrders = updated,
                        hasMore = hasMore,
                        lastOrderTimestamp = lastTs ?: st.lastOrderTimestamp,
                        filteredOrders = updated.applyFilters(st.selectedFilter, st.startDate, st.endDate)
                    )
                }
            } else {
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    fun setFilter(filter: String) {
        _uiState.update { state ->
            state.copy(
                selectedFilter = filter,
                filteredOrders = state.allOrders.applyFilters(filter, state.startDate, state.endDate)
            )
        }
    }

    fun setDateFilter(startDate: Long?, endDate: Long?) {
        _uiState.update { state ->
            state.copy(
                startDate = startDate,
                endDate = endDate,
                filteredOrders = state.allOrders.applyFilters(state.selectedFilter, startDate, endDate)
            )
        }
    }

    fun clearDateFilter() {
        _uiState.update { state ->
            state.copy(
                startDate = null,
                endDate = null,
                filteredOrders = state.allOrders.applyFilters(state.selectedFilter, null, null)
            )
        }
    }
}
