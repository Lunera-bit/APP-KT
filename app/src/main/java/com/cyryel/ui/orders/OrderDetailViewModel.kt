package com.cyryel.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyryel.data.order.OrderRepository
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderDetailUiState())
    val uiState: StateFlow<OrderDetailUiState> = _uiState.asStateFlow()

    fun loadOrder(orderId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = orderRepository.getOrderById(orderId)
            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false, order = result.getOrNull()) }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Error al cargar orden"
                    )
                }
            }
        }
    }

    fun cancelOrder(orderId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCancelling = true, errorMessage = null) }
            try {
                val functions = FirebaseFunctions.getInstance()
                val result = functions.getHttpsCallable("updateOrderStatus")
                    .call(mapOf("orderId" to orderId, "newStatus" to "cancelado"))
                    .await()
                _uiState.update {
                    it.copy(
                        isCancelling = false,
                        cancelSuccess = true,
                        order = it.order?.copy(status = "cancelado")
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isCancelling = false,
                        errorMessage = e.localizedMessage ?: "No se pudo cancelar el pedido"
                    )
                }
            }
        }
    }
}
