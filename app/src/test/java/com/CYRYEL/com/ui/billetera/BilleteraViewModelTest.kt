package com.CYRYEL.com.ui.billetera

import app.cash.turbine.test
import com.CYRYEL.com.TestCoroutineRule
import com.CYRYEL.com.data.auth.AuthRepository
import com.CYRYEL.com.data.cart.CartManager
import com.CYRYEL.com.data.product.Product
import com.CYRYEL.com.data.product.ProductRepository
import com.CYRYEL.com.data.promotion.Promotion
import com.CYRYEL.com.data.promotion.PromotionRepository
import com.CYRYEL.com.data.user.User
import com.CYRYEL.com.data.user.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BilleteraViewModelTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    @RelaxedMockK
    private lateinit var userRepository: UserRepository

    @RelaxedMockK
    private lateinit var promotionRepository: PromotionRepository

    @RelaxedMockK
    private lateinit var productRepository: ProductRepository

    @RelaxedMockK
    private lateinit var authRepository: AuthRepository

    @RelaxedMockK
    private lateinit var firestore: FirebaseFirestore

    @RelaxedMockK
    private lateinit var cartManager: CartManager

    private lateinit var viewModel: BilleteraViewModel

    private val sampleUser = User(
        id = "user-123", name = "Juan Perez", points = 500, email = "juan@test.com"
    )

    private val sampleOffers = listOf(
        Promotion(id = "promo-1", name = "Oferta 1", isActive = true)
    )

    private val sampleRedeemable = listOf(
        Product(id = "r1", nombre = "Canjeable 1", precio = 10.0, stock = 20, codigo = "R001", pointsToRedeem = 100),
        Product(id = "r2", nombre = "Canjeable 2", precio = 20.0, stock = 0, codigo = "R002", pointsToRedeem = 200)
    )

    @Before
    fun setUp() {
        every { authRepository.getCurrentUserId() } returns "user-123"
        every { promotionRepository.getActivePromotions() } returns flowOf(sampleOffers)
        coEvery { userRepository.getUser("user-123") } returns Result.success(sampleUser)
        coEvery { productRepository.getRedeemableProducts() } returns Result.success(sampleRedeemable)

        viewModel = BilleteraViewModel(
            userRepository, promotionRepository, productRepository,
            authRepository, firestore, cartManager
        )
    }

    @Test
    fun `init loads user and offers`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertNull(state.errorMessage)
            assertEquals(500, state.user?.points)
            assertEquals(1, state.offers.size)
        }
    }

    @Test
    fun `init loads redeemable products`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.redeemableProducts.size)
        }
    }

    @Test
    fun `loadBilletera handles failure`() = runTest {
        coEvery { userRepository.getUser("user-123") } returns Result.failure(Exception("Usuario no encontrado"))

        viewModel.loadBilletera()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Usuario no encontrado", state.errorMessage)
        }
    }

    @Test
    fun `redeemProduct with enough points adds to cart`() {
        val product = sampleRedeemable[0]

        val result = viewModel.redeemProduct(product)

        assertTrue(result)
    }

    @Test
    fun `redeemProduct with insufficient points returns false`() {
        val expensiveProduct = Product(
            id = "r3", nombre = "Caro", precio = 50.0, stock = 10,
            codigo = "R003", pointsToRedeem = 1000
        )

        val result = viewModel.redeemProduct(expensiveProduct)

        assertFalse(result)
    }

    @Test
    fun `redeemProduct with no stock returns false`() {
        val result = viewModel.redeemProduct(sampleRedeemable[1])

        assertFalse(result)
    }

    @Test
    fun `redeemProduct with zero user points returns false`() {
        coEvery { userRepository.getUser("user-123") } returns Result.success(sampleUser.copy(points = 0))

        val vm = BilleteraViewModel(
            userRepository, promotionRepository, productRepository,
            authRepository, firestore, cartManager
        )

        val result = vm.redeemProduct(sampleRedeemable[0])
        assertFalse(result)
    }

    @Test
    fun `promotions are observed`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.offers.size)
        }
    }

    @Test
    fun `PointsRules returns correct values`() {
        val rules = PointsRules()
        assertEquals(10, rules.pointsPer300)
        assertEquals(30, rules.pointsPer600)
        assertEquals(5, rules.promoUnitPoints)
    }
}
