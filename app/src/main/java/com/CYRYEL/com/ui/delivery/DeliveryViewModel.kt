package com.CYRYEL.com.ui.delivery

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.CYRYEL.com.LocationUploaderService
import com.CYRYEL.com.data.auth.AuthRepository
import com.CYRYEL.com.data.delivery.DeliveryAssignment
import com.CYRYEL.com.data.delivery.DeliveryRepository
import com.CYRYEL.com.data.order.Order
import com.CYRYEL.com.data.user.UserRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class DeliveryUiState(
    val isLoading: Boolean = false,
    val loadingMessage: String? = null,
    val isAvailable: Boolean = true,
    val availableDeliveries: List<Pair<DeliveryAssignment, Order>> = emptyList(),
    val myDeliveries: List<Pair<DeliveryAssignment, Order>> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val selectedDelivery: Pair<DeliveryAssignment, Order>? = null,
    val liveDeliveryLatitude: Double? = null,
    val liveDeliveryLongitude: Double? = null,
    val showPaymentDialog: Boolean = false,
    val pendingDeliveryId: String? = null,
    val pendingOrderId: String? = null,
    val pendingConfirmationCode: String? = null
)

@HiltViewModel
class DeliveryViewModel @Inject constructor(
    @ApplicationContext private val app: Context,
    private val deliveryRepository: DeliveryRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val fusedLocationClient: FusedLocationProviderClient,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeliveryUiState())
    val uiState: StateFlow<DeliveryUiState> = _uiState.asStateFlow()

    private var deliverySnapshotRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private var orderSnapshotRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        loadDeliveries()
        loadUserAvailability()
    }

    private fun loadUserAvailability() {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUserId() ?: return@launch
            userRepository.getUser(uid).onSuccess { user ->
                _uiState.update { it.copy(isAvailable = user.isAvailable) }
            }
        }
    }

    fun toggleAvailability() {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUserId() ?: return@launch
            val newState = !_uiState.value.isAvailable
            _uiState.update { it.copy(isAvailable = newState) }

            userRepository.updateUser(uid, mapOf("isAvailable" to newState, "updatedAt" to System.currentTimeMillis()))
                .onFailure {
                    _uiState.update { it.copy(isAvailable = !newState) }
                }
        }
    }

    fun loadDeliveries() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            deliveryRepository.getAvailableDeliveries().onSuccess { list ->
                _uiState.update { it.copy(availableDeliveries = list) }
            }.onFailure { e ->
                _uiState.update { it.copy(errorMessage = e.localizedMessage ?: "Error al cargar pedidos") }
            }

            val uid = authRepository.getCurrentUserId()
            if (uid != null) {
                deliveryRepository.getMyDeliveries(uid).onSuccess { list ->
                    _uiState.update { it.copy(myDeliveries = list) }
                }
            }

            val currentState = _uiState.value
            val selected = currentState.selectedDelivery
            if (selected != null) {
                val updated = currentState.myDeliveries.find {
                    it.first.id == selected.first.id
                } ?: currentState.availableDeliveries.find {
                    it.first.id == selected.first.id
                }
                if (updated != null && updated != selected) {
                    _uiState.update { it.copy(selectedDelivery = updated) }
                }
            }

            val activeDeliveries = _uiState.value.myDeliveries.any {
                it.first.status == "aceptado" || it.first.status == "en_camino"
            }
            val hasLocationPermission = ContextCompat.checkSelfPermission(
                app, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (activeDeliveries && hasLocationPermission) {
                LocationUploaderService.start(app)
            } else if (!activeDeliveries) {
                LocationUploaderService.stop(app)
            }

            _uiState.update { it.copy(isLoading = false, loadingMessage = null) }
        }
    }

    fun acceptDelivery(deliveryId: String, orderId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Aceptando pedido...", errorMessage = null) }

            val uid = authRepository.getCurrentUserId() ?: run {
                _uiState.update { it.copy(isLoading = false, loadingMessage = null, errorMessage = "Usuario no autenticado") }
                return@launch
            }

            val location = getCurrentLocation()
            if (location == null) {
                _uiState.update { it.copy(isLoading = false, loadingMessage = null, errorMessage = "No se pudo obtener la ubicacion") }
                return@launch
            }

            userRepository.getUser(uid).onSuccess { user ->
                val fcmToken = authRepository.getFcmToken(uid) ?: ""

                deliveryRepository.acceptDelivery(
                    deliveryId = deliveryId,
                    orderId = orderId,
                    deliveryPersonId = uid,
                    deliveryPersonName = user.name,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    fcmToken = fcmToken
                ).onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false, loadingMessage = null,
                            successMessage = "Pedido aceptado con exito"
                        )
                    }
                    loadDeliveries()
                }.onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false, loadingMessage = null,
                            errorMessage = e.localizedMessage ?: "Error al aceptar pedido"
                        )
                    }
                }
            }.onFailure {
                _uiState.update {
                    it.copy(isLoading = false, loadingMessage = null, errorMessage = "Error al obtener datos del usuario")
                }
            }
        }
    }

    fun startDelivery(deliveryId: String, orderId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Iniciando delivery...", errorMessage = null) }

            deliveryRepository.startDelivery(deliveryId, orderId).onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false, loadingMessage = null,
                        successMessage = "Pedido marcado como en camino"
                    )
                }
                loadDeliveries()
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false, loadingMessage = null,
                        errorMessage = e.localizedMessage ?: "Error al iniciar delivery"
                    )
                }
            }
        }
    }

    fun completeDelivery(deliveryId: String, orderId: String, confirmationCode: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Verificando codigo...", errorMessage = null) }

            deliveryRepository.verifyConfirmationCode(deliveryId, confirmationCode).onSuccess {
                val paymentStatus = _uiState.value.selectedDelivery?.second?.paymentStatus ?: "pendiente"
                val showDialog = paymentStatus != "completado"

                if (showDialog) {
                    _uiState.update {
                        it.copy(
                            isLoading = false, loadingMessage = null,
                            showPaymentDialog = true,
                            pendingDeliveryId = deliveryId,
                            pendingOrderId = orderId,
                            pendingConfirmationCode = confirmationCode
                        )
                    }
                } else {
                    deliveryRepository.completeDelivery(deliveryId, orderId, confirmationCode, paymentCompleted = true).onSuccess {
                        _uiState.update {
                            it.copy(
                                isLoading = false, loadingMessage = null,
                                successMessage = "Pedido entregado con exito",
                                selectedDelivery = null
                            )
                        }
                        loadDeliveries()
                    }.onFailure { e ->
                        _uiState.update {
                            it.copy(
                                isLoading = false, loadingMessage = null,
                                errorMessage = e.localizedMessage ?: "Error al completar delivery"
                            )
                        }
                    }
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false, loadingMessage = null,
                        errorMessage = e.localizedMessage ?: "Error al completar delivery"
                    )
                }
            }
        }
    }

    fun confirmPayment(completed: Boolean) {
        val deliveryId = _uiState.value.pendingDeliveryId
        val orderId = _uiState.value.pendingOrderId
        val code = _uiState.value.pendingConfirmationCode

        _uiState.update { it.copy(showPaymentDialog = false, pendingDeliveryId = null, pendingOrderId = null, pendingConfirmationCode = null) }

        if (!completed || deliveryId == null || orderId == null || code == null) {
            _uiState.update { it.copy(errorMessage = "Pago no confirmado por el repartidor. Vuelve a intentar cuando el cliente realice el pago.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Completando entrega...") }

            deliveryRepository.completeDelivery(deliveryId, orderId, code, paymentCompleted = true).onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false, loadingMessage = null,
                        successMessage = "Pedido entregado con exito",
                        selectedDelivery = null
                    )
                }
                loadDeliveries()
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false, loadingMessage = null,
                        errorMessage = e.localizedMessage ?: "Error al completar delivery"
                    )
                }
            }
        }
    }

    fun selectDelivery(assignment: DeliveryAssignment, order: Order) {
        _uiState.update {
            it.copy(
                selectedDelivery = Pair(assignment, order),
                liveDeliveryLatitude = assignment.lastLocationLatitude,
                liveDeliveryLongitude = assignment.lastLocationLongitude
            )
        }
        startWatchingDelivery(assignment.id)
        startWatchingOrder(order.id)
    }

    fun clearSelection() {
        deliverySnapshotRegistration?.remove()
        deliverySnapshotRegistration = null
        orderSnapshotRegistration?.remove()
        orderSnapshotRegistration = null
        _uiState.update { it.copy(selectedDelivery = null, liveDeliveryLatitude = null, liveDeliveryLongitude = null) }
    }

    private fun startWatchingDelivery(deliveryId: String) {
        deliverySnapshotRegistration?.remove()
        deliverySnapshotRegistration = firestore.collection("deliveries").document(deliveryId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                val location = snapshot.getGeoPoint("lastLocation")
                if (location != null) {
                    _uiState.update {
                        it.copy(
                            liveDeliveryLatitude = location.latitude,
                            liveDeliveryLongitude = location.longitude
                        )
                    }
                }
            }
    }

    private fun startWatchingOrder(orderId: String) {
        orderSnapshotRegistration?.remove()
        orderSnapshotRegistration = firestore.collection("orders").document(orderId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                val newPaymentStatus = snapshot.getString("paymentStatus")
                val current = _uiState.value.selectedDelivery ?: return@addSnapshotListener
                if (newPaymentStatus != null && newPaymentStatus != current.second.paymentStatus) {
                    _uiState.update {
                        it.copy(selectedDelivery = Pair(current.first, current.second.copy(paymentStatus = newPaymentStatus)))
                    }
                }
            }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    private suspend fun getCurrentLocation(): Location? {
        return try {
            val task = fusedLocationClient.lastLocation
            task.await()
        } catch (_: Exception) {
            null
        }
    }
}
