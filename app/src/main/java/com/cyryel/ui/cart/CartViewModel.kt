package com.cyryel.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyryel.data.auth.AuthRepository
import com.cyryel.data.cart.CartManager
import com.cyryel.data.order.CreateOrderRequest
import com.cyryel.data.order.OrderRepository
import com.cyryel.data.product.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartManager: CartManager,
    private val orderRepository: OrderRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CartUiState())
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            cartManager.items.collect { items ->
                _uiState.update {
                    it.copy(items = items, orderCreatedMessage = null)
                }
            }
        }
    }

    fun addProduct(product: Product) {
        cartManager.addProduct(product)
    }

    fun decreaseProduct(productId: String) {
        cartManager.decreaseProduct(productId)
    }

    fun removeProduct(productId: String) {
        cartManager.removeProduct(productId)
    }

    fun onStreetChange(value: String) {
        _uiState.update { it.copy(street = value, fieldErrors = it.fieldErrors - "street", errorMessage = null) }
    }

    fun onCityChange(value: String) {
        _uiState.update { it.copy(city = value, fieldErrors = it.fieldErrors - "city", errorMessage = null) }
    }

    fun onRecipientChange(value: String) {
        _uiState.update { it.copy(recipientName = value, fieldErrors = it.fieldErrors - "recipientName", errorMessage = null) }
    }

    fun onPhoneChange(value: String) {
        _uiState.update { it.copy(phone = value, fieldErrors = it.fieldErrors - "phone", errorMessage = null) }
    }

    fun onNotesChange(value: String) {
        _uiState.update { it.copy(notes = value, errorMessage = null) }
    }

    fun onDeliveryMethodChange(value: String) {
        _uiState.update { it.copy(deliveryMethod = value, errorMessage = null) }
    }

    fun placeOrder() {
        val state = _uiState.value
        val userId = authRepository.getCurrentUserId()
        val errors = mutableMapOf<String, String>()

        if (state.items.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "El carrito esta vacio") }
            return
        }

        if (state.street.isBlank()) errors["street"] = "Ingresa la direccion"
        if (state.city.isBlank()) errors["city"] = "Ingresa la ciudad"
        if (state.recipientName.isBlank()) errors["recipientName"] = "Ingresa el nombre del receptor"
        if (state.phone.isBlank()) {
            errors["phone"] = "Ingresa el telefono"
        } else if (!state.phone.matches(Regex("^9\\d{8}$"))) {
            errors["phone"] = "Telefono debe ser 9 digitos empezando con 9"
        }

        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(fieldErrors = errors) }
            return
        }

        if (userId == null) {
            _uiState.update { it.copy(errorMessage = "Debes iniciar sesion nuevamente") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isPlacingOrder = true, errorMessage = null, orderCreatedMessage = null, fieldErrors = emptyMap()) }

            val fcmToken = authRepository.getFcmToken(userId) ?: ""
            val userEmail = authRepository.getCurrentUserEmail().orEmpty()

            val result = orderRepository.createOrder(
                CreateOrderRequest(
                    userId = userId,
                    userEmail = userEmail,
                    items = state.items,
                    street = state.street,
                    city = state.city,
                    recipientName = state.recipientName,
                    phone = state.phone,
                    notes = state.notes,
                    shipping = 0.0,
                    deliveryMethod = state.deliveryMethod,
                    fcmToken = fcmToken
                )
            )

            if (result.isSuccess) {
                val orderId = result.getOrDefault("")
                cartManager.clear()
                _uiState.update {
                    it.copy(
                        isPlacingOrder = false,
                        orderCreatedMessage = "Pedido creado: $orderId",
                        notes = ""
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isPlacingOrder = false,
                        errorMessage = result.exceptionOrNull()?.localizedMessage ?: "No se pudo crear el pedido"
                    )
                }
            }
        }
    }
}
