package com.cyryel.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyryel.data.order.OrderRepository
import com.google.firebase.firestore.FirebaseFirestore
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
    private val orderRepository: OrderRepository,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderDetailUiState())
    val uiState: StateFlow<OrderDetailUiState> = _uiState.asStateFlow()

    private var deliverySnapshotRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    fun loadOrder(orderId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = orderRepository.getOrderById(orderId)
            if (result.isSuccess) {
                val order = result.getOrNull()
                _uiState.update { it.copy(isLoading = false, order = order) }
                if (order != null) {
                    startWatchingDeliveryLocation(orderId)
                    loadPromotionNames(order)
                }
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

    private suspend fun loadPromotionNames(order: Order) {
        val promoIds = order.items.mapNotNull { it.promotionId?.takeIf { id -> id.isNotBlank() } }.distinct()
        if (promoIds.isEmpty()) return
        val data = mutableMapOf<String, PromotionBrief>()
        for (id in promoIds) {
            try {
                val doc = firestore.collection("promotions").document(id).get().await()
                val name = doc.getString("name") ?: doc.getString("nombre") ?: id.take(8)
                val productsRaw = doc.get("products")
                val productQuantities = if (productsRaw is List<*>) {
                    productsRaw
                        .filterIsInstance<Map<*, *>>()
                        .mapNotNull { m ->
                            val pid = m["productId"] as? String
                            val qty = (m["quantity"] as? Number)?.toInt()
                            if (pid != null && qty != null) pid to qty else null
                        }
                        .toMap()
                } else emptyMap()
                data[id] = PromotionBrief(name = name, productQuantities = productQuantities)
            } catch (_: Exception) {
                data[id] = PromotionBrief(name = id.take(8))
            }
        }
        _uiState.update { it.copy(promotionData = data) }
    }

    private fun startWatchingDeliveryLocation(orderId: String) {
        deliverySnapshotRegistration?.remove()
        deliverySnapshotRegistration = firestore.collection("deliveries")
            .whereEqualTo("orderId", orderId)
            .whereIn("status", listOf("aceptado", "en_camino"))
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || snapshot.documents.isEmpty()) return@addSnapshotListener
                val doc = snapshot.documents[0]
                val location = doc.getGeoPoint("lastLocation")
                if (location != null) {
                    _uiState.update {
                        it.copy(
                            deliveryLatitude = location.latitude,
                            deliveryLongitude = location.longitude
                        )
                    }
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        deliverySnapshotRegistration?.remove()
    }

    fun cancelOrder(orderId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCancelling = true, errorMessage = null) }
            try {
                val functions = FirebaseFunctions.getInstance()
                functions.getHttpsCallable("updateOrderStatus")
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
