package com.CYRYEL.com.ui.checkout

import app.cash.turbine.test
import com.CYRYEL.com.TestCoroutineRule
import com.CYRYEL.com.data.auth.AuthRepository
import com.CYRYEL.com.data.cart.CartItem
import com.CYRYEL.com.data.cart.CartManager
import com.CYRYEL.com.data.config.BankAccountData
import com.CYRYEL.com.data.config.ConfigRepository
import com.CYRYEL.com.data.config.DeliveryConfigData
import com.CYRYEL.com.data.order.CreateOrderRequest
import com.CYRYEL.com.data.order.OrderRepository
import com.CYRYEL.com.data.product.Product
import com.CYRYEL.com.data.user.Address
import com.CYRYEL.com.data.user.User
import com.CYRYEL.com.data.user.UserRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import io.mockk.just
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CheckoutViewModelTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    @RelaxedMockK
    private lateinit var cartManager: CartManager

    @RelaxedMockK
    private lateinit var orderRepository: OrderRepository

    @RelaxedMockK
    private lateinit var authRepository: AuthRepository

    @RelaxedMockK
    private lateinit var userRepository: UserRepository

    @RelaxedMockK
    private lateinit var configRepository: ConfigRepository

    private lateinit var viewModel: CheckoutViewModel

    private val sampleProduct = Product(
        id = "prod-1", nombre = "Producto Test", precio = 25.0, stock = 20, codigo = "P001"
    )

    private val cartItem = CartItem(
        productId = "prod-1",
        productName = "Producto Test",
        quantity = 2,
        price = 25.0,
        subtotal = 50.0,
        product = sampleProduct
    )

    private val sampleUser = User(
        id = "user-123",
        name = "Juan Perez",
        phone = "987654321",
        documentNumber = "12345678",
        ruc = "",
        addresses = listOf(
            Address(id = "addr-1", street = "Av. Principal 123", city = "Chancay", isDefault = true)
        )
    )

    @Before
    fun setUp() {
        every { cartManager.items } returns kotlinx.coroutines.flow.MutableStateFlow(emptyList())
        every { authRepository.getCurrentUserId() } returns "user-123"
        every { authRepository.getCurrentUserEmail() } returns "juan@test.com"
        coEvery { authRepository.getFcmToken(any()) } returns "fcm-token"
        coEvery { userRepository.getUser("user-123") } returns Result.success(sampleUser)
        coEvery { configRepository.getBankAccounts() } returns Result.success(
            listOf(BankAccountData("BCP", "123-456"))
        )
        coEvery { configRepository.getDeliveryConfig() } returns Result.success(
            DeliveryConfigData(4.50, 1.30)
        )
        every { cartManager.items } returns kotlinx.coroutines.flow.MutableStateFlow(
            listOf(cartItem)
        )
        coEvery { orderRepository.createOrder(any()) } returns Result.success("order-123")

        viewModel = CheckoutViewModel(
            androidx.lifecycle.SavedStateHandle(),
            cartManager, orderRepository, authRepository, userRepository, configRepository
        )
    }

    @Test
    fun `init loads user profile`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoadingProfile)
            assertEquals("Juan Perez", state.recipientName)
            assertEquals("987654321", state.phone)
            assertEquals("Av. Principal 123", state.street)
        }
    }

    @Test
    fun `init loads bank accounts`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.bankAccounts.size)
            assertEquals("BCP", state.bankAccounts[0].name)
        }
    }

    @Test
    fun `nextStep from REVIEW with empty cart shows error`() = runTest {
        every { cartManager.items } returns kotlinx.coroutines.flow.MutableStateFlow(emptyList())

        val vm = CheckoutViewModel(
            androidx.lifecycle.SavedStateHandle(),
            cartManager, orderRepository, authRepository, userRepository, configRepository
        )

        vm.nextStep()

        vm.uiState.test {
            val state = awaitItem()
            assertEquals("El carrito esta vacio", state.errorMessage)
            assertEquals(CheckoutStep.REVIEW, state.currentStep)
        }
    }

    @Test
    fun `nextStep from REVIEW with items goes to DELIVERY`() = runTest {
        viewModel.nextStep()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(CheckoutStep.DELIVERY, state.currentStep)
            assertEquals(1, state.highestStepOrdinal)
        }
    }

    @Test
    fun `nextStep from DELIVERY with domicilio and blank address shows error`() = runTest {
        viewModel.goToStep(CheckoutStep.DELIVERY)
        viewModel.onStreetChange("")
        viewModel.onCityChange("")

        viewModel.nextStep()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(CheckoutStep.DELIVERY, state.currentStep)
            assertTrue(state.fieldErrors.containsKey("street"))
            assertTrue(state.fieldErrors.containsKey("city"))
        }
    }

    @Test
    fun `nextStep from DELIVERY with valid address proceeds`() = runTest {
        viewModel.goToStep(CheckoutStep.DELIVERY)
        viewModel.onStreetChange("Av. Test 123")
        viewModel.onCityChange("Chancay")

        viewModel.nextStep()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(CheckoutStep.CONTACT, state.currentStep)
        }
    }

    @Test
    fun `nextStep from CONTACT with blank name shows error`() = runTest {
        viewModel.goToStep(CheckoutStep.CONTACT)
        viewModel.onRecipientChange("")
        viewModel.onPhoneChange("987654321")
        viewModel.onDocumentTypeChange("dni")
        viewModel.onDocumentNumberChange("12345678")

        viewModel.nextStep()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(CheckoutStep.CONTACT, state.currentStep)
            assertTrue(state.fieldErrors.containsKey("recipientName"))
        }
    }

    @Test
    fun `nextStep from CONTACT with invalid phone shows error`() = runTest {
        viewModel.goToStep(CheckoutStep.CONTACT)
        viewModel.onRecipientChange("Juan")
        viewModel.onPhoneChange("123")
        viewModel.onDocumentTypeChange("dni")
        viewModel.onDocumentNumberChange("12345678")

        viewModel.nextStep()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(CheckoutStep.CONTACT, state.currentStep)
            assertTrue(state.fieldErrors.containsKey("phone"))
        }
    }

    @Test
    fun `nextStep from CONTACT with invalid DNI shows error`() = runTest {
        viewModel.goToStep(CheckoutStep.CONTACT)
        viewModel.onRecipientChange("Juan")
        viewModel.onPhoneChange("987654321")
        viewModel.onDocumentTypeChange("dni")
        viewModel.onDocumentNumberChange("123")

        viewModel.nextStep()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(CheckoutStep.CONTACT, state.currentStep)
            assertTrue(state.fieldErrors.containsKey("documentNumber"))
        }
    }

    @Test
    fun `nextStep from CONTACT with valid info proceeds`() = runTest {
        viewModel.goToStep(CheckoutStep.CONTACT)
        viewModel.onRecipientChange("Juan Perez")
        viewModel.onPhoneChange("987654321")
        viewModel.onDocumentTypeChange("dni")
        viewModel.onDocumentNumberChange("12345678")

        viewModel.nextStep()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(CheckoutStep.PAYMENT, state.currentStep)
        }
    }

    @Test
    fun `nextStep from PAYMENT with no method shows error`() = runTest {
        viewModel.goToStep(CheckoutStep.CONTACT)
        viewModel.onRecipientChange("Juan Perez")
        viewModel.onPhoneChange("987654321")
        viewModel.onDocumentTypeChange("dni")
        viewModel.onDocumentNumberChange("12345678")
        viewModel.nextStep()
        viewModel.onPaymentMethodChange("")

        viewModel.nextStep()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(CheckoutStep.PAYMENT, state.currentStep)
            assertEquals("Selecciona un metodo de pago", state.errorMessage)
        }
    }

    @Test
    fun `full checkout flow to CONFIRM`() = runTest {
        viewModel.goToStep(CheckoutStep.CONTACT)
        viewModel.onRecipientChange("Juan Perez")
        viewModel.onPhoneChange("987654321")
        viewModel.onDocumentTypeChange("dni")
        viewModel.onDocumentNumberChange("12345678")
        viewModel.nextStep()

        viewModel.onPaymentMethodChange("contra_entrega")
        viewModel.nextStep()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(CheckoutStep.CONFIRM, state.currentStep)
            assertEquals(4, state.highestStepOrdinal)
        }
    }

    @Test
    fun `placeOrder creates order successfully`() = runTest {
        viewModel.goToStep(CheckoutStep.CONTACT)
        viewModel.onRecipientChange("Juan Perez")
        viewModel.onPhoneChange("987654321")
        viewModel.onDocumentTypeChange("dni")
        viewModel.onDocumentNumberChange("12345678")
        viewModel.nextStep()
        viewModel.onPaymentMethodChange("contra_entrega")
        viewModel.nextStep()

        viewModel.placeOrder()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isPlacingOrder)
            assertEquals("order-123", state.orderId)
            assertNotNull(state.orderCreatedMessage)
        }
        verify { orderRepository.createOrder(any()) }
        verify { cartManager.clear() }
    }

    @Test
    fun `placeOrder with empty cart shows error`() = runTest {
        every { cartManager.items } returns kotlinx.coroutines.flow.MutableStateFlow(emptyList())

        val vm = CheckoutViewModel(
            androidx.lifecycle.SavedStateHandle(),
            cartManager, orderRepository, authRepository, userRepository, configRepository
        )

        vm.placeOrder()

        vm.uiState.test {
            val state = awaitItem()
            assertEquals("El carrito esta vacio", state.errorMessage)
            assertFalse(state.isPlacingOrder)
        }
    }

    @Test
    fun `placeOrder with null userId shows error`() = runTest {
        every { authRepository.getCurrentUserId() } returns null

        viewModel.placeOrder()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Debes iniciar sesion nuevamente", state.errorMessage)
        }
    }

    @Test
    fun `placeOrder handles failure`() = runTest {
        coEvery { orderRepository.createOrder(any()) } returns Result.failure(Exception("Error de red"))
        viewModel.goToStep(CheckoutStep.CONTACT)
        viewModel.onRecipientChange("Juan Perez")
        viewModel.onPhoneChange("987654321")
        viewModel.onDocumentTypeChange("dni")
        viewModel.onDocumentNumberChange("12345678")
        viewModel.nextStep()
        viewModel.onPaymentMethodChange("contra_entrega")
        viewModel.nextStep()

        viewModel.placeOrder()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isPlacingOrder)
            assertEquals("Error de red", state.errorMessage)
        }
    }

    @Test
    fun `onDeliveryMethodChange to tienda updates location to store`() = runTest {
        viewModel.onDeliveryMethodChange("tienda")

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("tienda", state.deliveryMethod)
            assertEquals(StoreCoordinates.LATITUDE, state.latitude, 0.001)
            assertEquals(StoreCoordinates.LONGITUDE, state.longitude, 0.001)
            assertEquals(0.0, state.deliveryCost, 0.001)
        }
    }

    @Test
    fun `onDeliveryMethodChange to domicilio restores saved address`() = runTest {
        viewModel.onDeliveryMethodChange("tienda")
        viewModel.onDeliveryMethodChange("domicilio")

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("domicilio", state.deliveryMethod)
            assertEquals("Av. Principal 123", state.street)
        }
    }

    @Test
    fun `selectAddress updates address fields`() {
        val address = Address(
            id = "addr-2", street = "Jr. Los Olivos 456",
            city = "Huaral", reference = "Cerca al mercado",
            latitude = -11.5, longitude = -77.2
        )

        viewModel.selectAddress(address)

        val state = viewModel.uiState.value
        assertEquals("Jr. Los Olivos 456", state.street)
        assertEquals("Huaral", state.city)
        assertEquals("Cerca al mercado", state.reference)
        assertEquals(-11.5, state.latitude, 0.001)
        assertEquals(-77.2, state.longitude, 0.001)
    }

    @Test
    fun `goToStep goes back to previous step`() = runTest {
        viewModel.nextStep()
        viewModel.goToStep(CheckoutStep.REVIEW)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(CheckoutStep.REVIEW, state.currentStep)
        }
    }

    @Test
    fun `goToStep does not go forward beyond highestStepOrdinal`() = runTest {
        viewModel.goToStep(CheckoutStep.CONFIRM)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(CheckoutStep.REVIEW, state.currentStep)
        }
    }

    @Test
    fun `onPhoneChange filters only digits and limits to 9`() {
        viewModel.onPhoneChange("987abc654321xyz")
        assertEquals("987654321", viewModel.uiState.value.phone)
    }

    @Test
    fun `onDocumentNumberChange limits DNI to 8 digits`() {
        viewModel.onDocumentTypeChange("dni")
        viewModel.onDocumentNumberChange("1234567890")
        assertEquals("12345678", viewModel.uiState.value.documentNumber)
    }

    @Test
    fun `onDocumentNumberChange limits RUC to 11 digits`() {
        viewModel.onDocumentTypeChange("ruc")
        viewModel.onDocumentNumberChange("1234567890123")
        assertEquals("12345678901", viewModel.uiState.value.documentNumber)
    }

    @Test
    fun `deliveryCostFor returns zero for store coordinates`() {
        val cost = CheckoutViewModel.deliveryCostFor(
            StoreCoordinates.LATITUDE, StoreCoordinates.LONGITUDE, 4.50, 1.30
        )
        assertEquals(0.0, cost, 0.001)
    }

    @Test
    fun `deliveryCostFor returns zero for distance within free radius`() {
        val cost = CheckoutViewModel.deliveryCostFor(
            -11.567, -77.269, 4.50, 1.30
        )
        assertEquals(0.0, cost, 0.001)
    }

    @Test
    fun `deliveryCostFor returns rounded cost for longer distance`() {
        val cost = CheckoutViewModel.deliveryCostFor(
            -11.5, -77.0, 4.50, 1.30
        )
        assertTrue(cost > 0)
        assertEquals(cost, kotlin.math.round(cost * 2) / 2, 0.001)
    }

    @Test
    fun `createOrderRequest includes correct data`() = runTest {
        val requestSlot = slot<CreateOrderRequest>()

        coEvery { orderRepository.createOrder(capture(requestSlot)) } returns Result.success("order-456")

        viewModel.goToStep(CheckoutStep.CONTACT)
        viewModel.onRecipientChange("Juan Perez")
        viewModel.onPhoneChange("987654321")
        viewModel.onDocumentTypeChange("dni")
        viewModel.onDocumentNumberChange("12345678")
        viewModel.nextStep()
        viewModel.onPaymentMethodChange("contra_entrega")
        viewModel.nextStep()

        viewModel.placeOrder()

        val request = requestSlot.captured
        assertEquals("user-123", request.userId)
        assertEquals("Juan Perez", request.recipientName)
        assertEquals("987654321", request.phone)
        assertEquals("12345678", request.documentNumber)
        assertEquals(1, request.items.size)
    }
}
