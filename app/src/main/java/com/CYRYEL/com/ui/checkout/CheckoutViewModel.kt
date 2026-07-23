package com.CYRYEL.com.ui.checkout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.CYRYEL.com.data.auth.AuthRepository
import com.CYRYEL.com.data.cart.CartManager
import com.CYRYEL.com.data.config.BankAccountData
import com.CYRYEL.com.data.config.ConfigRepository
import com.CYRYEL.com.data.order.CreateOrderRequest
import com.CYRYEL.com.data.order.OrderRepository
import com.CYRYEL.com.data.user.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val cartManager: CartManager,
    private val orderRepository: OrderRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val configRepository: ConfigRepository
) : ViewModel() {

    companion object {
        private const val FREE_DELIVERY_RADIUS = 500.0

        private fun distanceInMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
            val R = 6371000.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLng = Math.toRadians(lng2 - lng1)
            val sinHalfLat = kotlin.math.sin(dLat / 2)
            val sinHalfLng = kotlin.math.sin(dLng / 2)
            val a = sinHalfLat * sinHalfLat +
                    kotlin.math.cos(Math.toRadians(lat1)) *
                    kotlin.math.cos(Math.toRadians(lat2)) *
                    sinHalfLng * sinHalfLng
            return R * 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        }

        fun deliveryCostFor(latitude: Double, longitude: Double, basePrice: Double, costPerKm: Double): Double {
            if (latitude == StoreCoordinates.LATITUDE && longitude == StoreCoordinates.LONGITUDE) return 0.0
            val distMeters = distanceInMeters(StoreCoordinates.LATITUDE, StoreCoordinates.LONGITUDE, latitude, longitude)
            if (distMeters <= FREE_DELIVERY_RADIUS) return 0.0
            val distKm = distMeters / 1000.0
            val raw = if (distKm <= 1.0) basePrice
                      else basePrice + ((distKm - 1.0) * costPerKm)
            return kotlin.math.round(raw * 2) / 2
        }
    }

    private var deliveryBasePrice: Double = 4.50
    private var deliveryCostPerKm: Double = 1.30
    private var userDni: String = ""
    private var userRuc: String = ""
    private var savedDeliveryStreet: String = ""
    private var savedDeliveryCity: String = ""
    private var savedDeliveryReference: String = ""
    private var savedDeliveryLatitude: Double = StoreCoordinates.LATITUDE
    private var savedDeliveryLongitude: Double = StoreCoordinates.LONGITUDE
    private var savedSelectedAddressId: String? = null

    private val _uiState = MutableStateFlow(
        CheckoutUiState(
            orderId = savedStateHandle.get<String>("orderId") ?: "",
            orderCreatedMessage = savedStateHandle.get<String>("orderCreatedMessage")
        )
    )
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    init {
        val prevOrderId = savedStateHandle.get<String>("orderId")
        if (!prevOrderId.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    currentStep = CheckoutStep.CONFIRM,
                    highestStepOrdinal = CheckoutStep.CONFIRM.ordinal,
                    orderId = prevOrderId,
                    orderCreatedMessage = savedStateHandle.get<String>("orderCreatedMessage")
                )
            }
        } else {
            loadUserProfile()
        }
        loadBankAccounts()
        loadDeliveryConfig()
        viewModelScope.launch {
            cartManager.items.collect { items ->
                _uiState.update {
                    if (it.orderId.isNotBlank()) it
                    else it.copy(items = items)
                }
            }
        }
    }

    private fun loadDeliveryConfig() {
        viewModelScope.launch {
            configRepository.getDeliveryConfig().onSuccess { config ->
                deliveryBasePrice = config.basePrice
                deliveryCostPerKm = config.costPerKm
                refreshDeliveryCost()
            }
        }
    }

    private fun loadBankAccounts() {
        viewModelScope.launch {
            val result = configRepository.getBankAccounts()
            result.onSuccess { accounts ->
                _uiState.update { it.copy(bankAccounts = accounts, bankTitular = "CYRYEL Eirl") }
            }
        }
    }

    private fun loadUserProfile() {
        val userId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            val result = userRepository.getUser(userId)
            result.onSuccess { user ->
                userDni = user.documentNumber
                userRuc = user.ruc
                val defaultDni = if (user.ruc.isNotBlank()) "ruc" else "dni"
                val defaultNumber = if (user.ruc.isNotBlank()) user.ruc else user.documentNumber
                val defaultAddr = user.addresses.firstOrNull { it.isDefault } ?: user.addresses.firstOrNull()
                _uiState.update {
                    it.copy(
                        recipientName = user.name,
                        phone = user.phone,
                        documentNumber = defaultNumber,
                        documentType = defaultDni,
                        street = defaultAddr?.street ?: "",
                        city = defaultAddr?.city ?: "",
                        reference = defaultAddr?.reference ?: "",
                        latitude = defaultAddr?.latitude ?: it.latitude,
                        longitude = defaultAddr?.longitude ?: it.longitude,
                        savedAddresses = user.addresses,
                        selectedAddressId = defaultAddr?.id,
                        isLoadingProfile = false
                    )
                }
                refreshDeliveryCost()
            }
            result.onFailure {
                _uiState.update { it.copy(isLoadingProfile = false) }
            }
        }
    }

    fun goToStep(step: CheckoutStep) {
        val state = _uiState.value
        if (step.ordinal <= state.highestStepOrdinal) {
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
            val jumpTo = maxOf(nextOrdinal, state.highestStepOrdinal)
            val newHighest = maxOf(state.highestStepOrdinal, jumpTo)
            _uiState.update {
                it.copy(
                    currentStep = CheckoutStep.entries[jumpTo],
                    highestStepOrdinal = newHighest,
                    fieldErrors = emptyMap(),
                    errorMessage = null
                )
            }
        }
    }

    fun onDeliveryMethodChange(value: String) {
        val current = _uiState.value
        if (value == "tienda" && current.deliveryMethod == "domicilio") {
            savedDeliveryStreet = current.street
            savedDeliveryCity = current.city
            savedDeliveryReference = current.reference
            savedDeliveryLatitude = current.latitude
            savedDeliveryLongitude = current.longitude
            savedSelectedAddressId = current.selectedAddressId
            _uiState.update {
                it.copy(
                    deliveryMethod = value,
                    street = "Calle Belen 310",
                    city = "Chancay",
                    reference = "",
                    latitude = StoreCoordinates.LATITUDE,
                    longitude = StoreCoordinates.LONGITUDE,
                    selectedAddressId = null,
                    errorMessage = null
                )
            }
        } else if (value == "domicilio" && current.deliveryMethod == "tienda") {
            _uiState.update {
                it.copy(
                    deliveryMethod = value,
                    street = savedDeliveryStreet,
                    city = savedDeliveryCity,
                    reference = savedDeliveryReference,
                    latitude = savedDeliveryLatitude,
                    longitude = savedDeliveryLongitude,
                    selectedAddressId = savedSelectedAddressId,
                    errorMessage = null
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    deliveryMethod = value,
                    latitude = if (value == "tienda") StoreCoordinates.LATITUDE else it.latitude,
                    longitude = if (value == "tienda") StoreCoordinates.LONGITUDE else it.longitude,
                    errorMessage = null
                )
            }
        }
        refreshDeliveryCost()
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
        val number = when (value) {
            "dni" -> userDni
            "ruc" -> userRuc
            else -> ""
        }
        _uiState.update { it.copy(documentType = value, documentNumber = number, fieldErrors = it.fieldErrors - "documentNumber", errorMessage = null) }
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

    private fun refreshDeliveryCost() {
        val s = _uiState.value
        val cost = if (s.deliveryMethod == "tienda") 0.0
                   else deliveryCostFor(s.latitude, s.longitude, deliveryBasePrice, deliveryCostPerKm)
        _uiState.update { it.copy(deliveryCost = cost) }
    }

    fun onLatitudeChange(value: Double) {
        _uiState.update { it.copy(latitude = value) }
        refreshDeliveryCost()
    }

    fun onLongitudeChange(value: Double) {
        _uiState.update { it.copy(longitude = value) }
        refreshDeliveryCost()
    }

    fun clearSelectedAddress() {
        _uiState.update { it.copy(selectedAddressId = null) }
    }

    fun selectAddress(address: com.CYRYEL.com.data.user.Address) {
        _uiState.update {
            it.copy(
                street = address.street,
                city = address.city,
                reference = address.reference,
                latitude = address.latitude,
                longitude = address.longitude,
                selectedAddressId = address.id,
                fieldErrors = it.fieldErrors - "street" - "city"
            )
        }
        refreshDeliveryCost()
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

            val pointsUsed = state.items
                .filter { it.redeemedByPoints }
                .sumOf { it.product.pointsToRedeem * it.quantity }
            val pointsDiscount = state.items
                .filter { it.redeemedByPoints }
                .sumOf { it.product.precio * it.quantity }

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
                    shipping = state.deliveryCost,
                    deliveryMethod = state.deliveryMethod,
                    paymentMethod = state.paymentMethod,
                    latitude = state.latitude,
                    longitude = state.longitude,
                    fcmToken = fcmToken,
                    pointsUsed = pointsUsed,
                    pointsDiscount = pointsDiscount
                )
            )

            if (result.isSuccess) {
                val orderId = result.getOrDefault("")
                val snapshot = OrderSnapshot(
                    items = state.items,
                    subtotal = state.subtotal,
                    deliveryMethod = state.deliveryMethod,
                    street = state.street,
                    city = state.city,
                    latitude = state.latitude,
                    longitude = state.longitude,
                    reference = state.reference,
                    recipientName = state.recipientName,
                    phone = state.phone,
                    notes = state.notes,
                    paymentMethod = state.paymentMethod,
                    pointsUsed = pointsUsed,
                    pointsDiscount = pointsDiscount
                )
                savedStateHandle["orderId"] = orderId
                savedStateHandle["orderCreatedMessage"] = "Pedido creado exitosamente"
                cartManager.clear()
                _uiState.update {
                    it.copy(
                        isPlacingOrder = false,
                        orderCreatedMessage = "Pedido creado exitosamente",
                        orderId = orderId,
                        lastOrderSnapshot = snapshot
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
