package com.CYRYEL.com.ui.promotiondetail

import app.cash.turbine.test
import com.CYRYEL.com.TestCoroutineRule
import com.CYRYEL.com.data.cart.CartManager
import com.CYRYEL.com.data.promotion.Promotion
import com.CYRYEL.com.data.promotion.PromotionRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
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
class PromotionDetailViewModelTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    @RelaxedMockK
    private lateinit var promotionRepository: PromotionRepository

    @RelaxedMockK
    private lateinit var cartManager: CartManager

    private lateinit var viewModel: PromotionDetailViewModel

    private val samplePromotion = Promotion(
        id = "promo-1",
        name = "Combo Ahorro",
        description = "Ahorra mucho",
        originalPrice = 100.0,
        finalPrice = 75.0,
        isActive = true,
        stockRemaining = 10
    )

    @Before
    fun setUp() {
        coEvery { promotionRepository.getPromotionById("promo-1") } returns samplePromotion
        viewModel = PromotionDetailViewModel(promotionRepository, cartManager)
    }

    @Test
    fun `loadPromotion loads successfully`() = runTest {
        viewModel.loadPromotion("promo-1")

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertNotNull(state.promotion)
            assertEquals("Combo Ahorro", state.promotion?.name)
            assertNull(state.errorMessage)
        }
    }

    @Test
    fun `loadPromotion with null promotion shows not found`() = runTest {
        coEvery { promotionRepository.getPromotionById("bad-id") } returns null

        viewModel.loadPromotion("bad-id")

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertNull(state.promotion)
            assertEquals("Promocion no encontrada", state.errorMessage)
        }
    }

    @Test
    fun `loadPromotion handles exception`() = runTest {
        coEvery { promotionRepository.getPromotionById("error-id") } throws Exception("Error de red")

        viewModel.loadPromotion("error-id")

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals("Error de red", state.errorMessage)
        }
    }

    @Test
    fun `addToCart adds promotion to cart`() = runTest {
        viewModel.loadPromotion("promo-1")
        viewModel.addToCart()

        verify { cartManager.addPromotionProducts(samplePromotion) }
    }

    @Test
    fun `addToCart does nothing when no promotion loaded`() {
        viewModel.addToCart()

        verify(inverse = true) { cartManager.addPromotionProducts(any()) }
    }
}
