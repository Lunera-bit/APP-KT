package com.cyryel.ui.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyryel.data.auth.AuthRepository
import com.cyryel.data.cart.CartManager
import com.cyryel.data.order.CreateOrderRequest
import com.cyryel.data.order.OrderRepository
import com.cyryel.data.user.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val cartManager: CartManager,
    private val orderRepository: OrderRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
        viewModelScope.launch {
            cartManager.items.collect { items ->
                _uiState.update { it.copy(items = items) }
            }
        }
    }

    private fun loadUserProfile() {
        val userId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            val result = userRepository.getUser(userId)
            result.onSuccess { user ->
                _uiState.update {
                    it.copy(
                        recipientName = user.name,
                        phone = user.phone,
                        documentNumber = if (user.ruc.isNotBlank()) user.ruc else user.documentNumber,
                        documentType = if (user.ruc.isNotBlank()) "ruc" else "dni",
                        street = user.addresses.firstOrNull()?.street ?: "",
                        city = user.addresses.firstOrNull()?.city ?: "",
                        isLoadingProfile = false
                    )
                }
            }
            result.onFailure {
                _uiState.update { it.copy(isLoadingProfile = false) }
            }
        }
    }

    fun goToStep(step: CheckoutStep) {
        if (step.ordinal < _uiState.value.currentStep.ordinal) {
            _uiState.update { it.copy(currentStep = step, errorMessage = null) }
        }
    }

    fun nextStep() {
        val state = _uiState.value
        val errors = mutableMapOf<String, String>()

        when (state.currentStep) {
            CheckoutStep.REVIEW -> {
                if (state.items.isEmpty()) {
                    _uiState.update { it.copy(errorMessage = "El carrito esta vacio") }
                    return
                }
            }
            CheckoutStep.DELIVERY -> {
                if (state.deliveryMethod == "domicilio") {
                    if (state.street.isBlank()) errors["street"] = "Ingresa la direccion"
                    if (state.city.isBlank()) errors["city"] = "Ingresa la ciudad"
                }
                if (errors.isNotEmpty()) {
                    _uiState.update { it.copy(fieldErrors = errors) }
                    return
                }
            }
            CheckoutStep.CONTACT -> {
                if (state.recipientName.isBlank()) errors["recipientName"] = "Ingresa el nombre"
                if (state.phone.isBlank()) {
                    errors["phone"] = "Ingresa el telefono"
                } else if (!state.phone.matches(Regex("^9\\d{8}$"))) {
                    errors["phone"] = "Telefono debe ser 9 digitos empezando con 9"
                }
                if (state.documentType == "dni") {
                    if (state.documentNumber.length != 8) {
                        errors["documentNumber"] = "DNI debe tener 8 digitos"
                    }
                } else {
                    if (state.documentNumber.length != 11) {
                        errors["documentNumber"] = "RUC debe tener 11 digitos"
                    }
                }
                if (errors.isNotEmpty()) {
                    _uiState.update { it.copy(fieldErrors = errors) }
                    return
                }
            }
            CheckoutStep.PAYMENT -> {
                if (state.paymentMethod.isBlank()) {
                    _uiState.update { it.copy(errorMessage = "Selecciona un metodo de pago") }
                    return
                }
            }
            CheckoutStep.CONFIRM -> {}
        }

        val nextOrdinal = state.currentStep.ordinal + 1
        if (nextOrdinal < CheckoutStep.entries.size) {
            _uiState.update {
                it.copy(
                    currentStep = CheckoutStep.entries[nextOrdinal],
                    fieldErrors = emptyMap(),
                    errorMessage = null
                )
            }
        }
    }

    fun onDeliveryMethodChange(value: String) {
        _uiState.update { it.copy(deliveryMethod = value, errorMessage = null) }
    }

    fun onStreetChange(value: String) {
        _uiState.update { it.copy(street = value, fieldErrors = it.fieldErrors - "street", errorMessage = null) }
    }

    fun onCityChange(value: String) {
        _uiState.update { it.copy(city = value, fieldErrors = it.fieldErrors - "city", errorMessage = null) }
    }

    fun onReferenceChange(value: String) {
        _uiState.update { it.copy(reference = value) }
    }

    fun onRecipientChange(value: String) {
        _uiState.update { it.copy(recipientName = value, fieldErrors = it.fieldErrors - "recipientName", errorMessage = null) }
    }

    fun onPhoneChange(value: String) {
        val filtered = value.filter { it.isDigit() }.take(9)
        _uiState.update { it.copy(phone = filtered, fieldErrors = it.fieldErrors - "phone", errorMessage = null) }
    }

    fun onDocumentTypeChange(value: String) {
        _uiState.update { it.copy(documentType = value, documentNumber = "", fieldErrors = it.fieldErrors - "documentNumber", errorMessage = null) }
    }

    fun onDocumentNumberChange(value: String) {
        val maxLen = if (_uiState.value.documentType == "dni") 8 else 11
        val filtered = value.filter { it.isDigit() }.take(maxLen)
        _uiState.update { it.copy(documentNumber = filtered, fieldErrors = it.fieldErrors - "documentNumber", errorMessage = null) }
    }

    fun onNotesChange(value: String) {
        _uiState.update { it.copy(notes = value) }
    }

    fun onPaymentMethodChange(value: String) {
        _uiState.update { it.copy(paymentMethod = value, errorMessage = null) }
    }

    fun placeOrder() {
        val state = _uiState.value
        val userId = authRepository.getCurrentUserId()

        if (state.items.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "El carrito esta vacio") }
            return
        }

        if (userId == null) {
            _uiState.update { it.copy(errorMessage = "Debes iniciar sesion nuevamente") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isPlacingOrder = true, errorMessage = null, orderCreatedMessage = null) }

            val fcmToken = authRepository.getFcmToken(userId) ?: ""
            val userEmail = authRepository.getCurrentUserEmail().orEmpty()

            val result = orderRepository.createOrder(
                CreateOrderRequest(
                    userId = userId,
                    userEmail = userEmail,
                    items = state.items,
                    street = state.street,
                    city = state.city,
                    reference = state.reference,
                    recipientName = state.recipientName,
                    phone = state.phone,
                    notes = state.notes,
                    documentType = state.documentType,
                    documentNumber = state.documentNumber,
                    shipping = 0.0,
                    deliveryMethod = state.deliveryMethod,
                    paymentMethod = state.paymentMethod,
                    latitude = state.latitude,
                    longitude = state.longitude,
                    fcmToken = fcmToken
                )
            )

            if (result.isSuccess) {
                val orderId = result.getOrDefault("")
                cartManager.clear()
                _uiState.update {
                    it.copy(
                        isPlacingOrder = false,
                        orderCreatedMessage = "Pedido creado exitosamente",
                        orderId = orderId
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
