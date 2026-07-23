package com.CYRYEL.com.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.CYRYEL.com.data.auth.AuthRepository
import com.CYRYEL.com.data.order.Order
import com.CYRYEL.com.data.order.OrderRepository
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
        val userId = authRepository.getCurrentUserId()
        if (userId != null) {
            viewModelScope.launch {
                orderRepository.observeOrdersByUserId(userId).collect { recent ->
                    _uiState.update { state ->
                        val recentIds = recent.map { it.id }.toSet()
                        val merged = recent + state.paginatedOrders.filter { it.id !in recentIds }
                        state.copy(
                            isLoading = false,
                            allOrders = merged,
                            filteredOrders = merged.applyFilters(state.selectedFilter, state.startDate, state.endDate)
                        )
                    }
                }
            }
        } else {
            _uiState.update { it.copy(isLoading = false, errorMessage = "Usuario no autenticado") }
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
                    val updatedPaginated = st.paginatedOrders + newOrders
                    val recentIds = st.allOrders.map { it.id }.toSet()
                    val merged = st.allOrders + updatedPaginated.filter { it.id !in recentIds }
                    st.copy(
                        isLoadingMore = false,
                        paginatedOrders = updatedPaginated,
                        allOrders = merged,
                        hasMore = hasMore,
                        lastOrderTimestamp = lastTs ?: st.lastOrderTimestamp,
                        filteredOrders = merged.applyFilters(st.selectedFilter, st.startDate, st.endDate)
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
