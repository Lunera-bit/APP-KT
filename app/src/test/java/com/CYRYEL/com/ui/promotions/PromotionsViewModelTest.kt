package com.CYRYEL.com.ui.promotions

import app.cash.turbine.test
import com.CYRYEL.com.TestCoroutineRule
import com.CYRYEL.com.data.promotion.Promotion
import com.CYRYEL.com.data.promotion.PromotionRepository
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PromotionsViewModelTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    @RelaxedMockK
    private lateinit var promotionRepository: PromotionRepository

    private lateinit var viewModel: PromotionsViewModel

    private val samplePromotions = listOf(
        Promotion(id = "p1", name = "Promo 1", isActive = true, finalPrice = 25.0),
        Promotion(id = "p2", name = "Promo 2", isActive = true, finalPrice = 50.0)
    )

    @Before
    fun setUp() {
        every { promotionRepository.getActivePromotions() } returns flowOf(samplePromotions)
        viewModel = PromotionsViewModel(promotionRepository)
    }

    @Test
    fun `init loads promotions`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(2, state.promotions.size)
            assertEquals("Promo 1", state.promotions[0].name)
        }
    }

    @Test
    fun `retryPromotions reloads promotions`() = runTest {
        viewModel.retryPromotions()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.promotions.size)
        }
    }
}
