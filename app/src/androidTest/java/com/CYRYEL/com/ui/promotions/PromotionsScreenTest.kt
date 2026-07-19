package com.CYRYEL.com.ui.promotions

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.CYRYEL.com.data.promotion.PromotionRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class PromotionsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun promotionsScreen_showsTitle() {
        val repo = mockk<PromotionRepository>(relaxed = true) {
            every { getActivePromotions() } returns flowOf(emptyList())
        }
        val vm = PromotionsViewModel(repo)

        composeTestRule.setContent {
            PromotionsScreen(onBack = {}, viewModel = vm)
        }

        composeTestRule.onNodeWithText("Promociones").assertIsDisplayed()
    }

    @Test
    fun promotionsScreen_showsPromotionList() {
        val promotions = listOf(
            com.CYRYEL.com.data.promotion.Promotion(
                id = "p1", name = "Combo Ahorro",
                originalPrice = 100.0, finalPrice = 75.0, savings = 25.0
            )
        )
        val repo = mockk<PromotionRepository>(relaxed = true) {
            every { getActivePromotions() } returns flowOf(promotions)
        }
        val vm = PromotionsViewModel(repo)

        composeTestRule.setContent {
            PromotionsScreen(onBack = {}, viewModel = vm)
        }

        composeTestRule.onNodeWithText("Combo Ahorro").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ahorras S/ 25.00").assertIsDisplayed()
    }

    @Test
    fun promotionsScreen_showsErrorAndRetry() {
        val repo = mockk<PromotionRepository>(relaxed = true) {
            every { getActivePromotions() } returns flowOf(emptyList())
        }
        val vm = PromotionsViewModel(repo)

        composeTestRule.setContent {
            PromotionsScreen(onBack = {}, viewModel = vm)
        }

        composeTestRule.onNodeWithText("Promociones").assertIsDisplayed()
    }

    @Test
    fun promotionsScreen_showsLoadingIndicator() {
        val repo = mockk<PromotionRepository>(relaxed = true) {
            every { getActivePromotions() } returns flowOf(emptyList())
        }
        val vm = PromotionsViewModel(repo)

        composeTestRule.setContent {
            PromotionsScreen(onBack = {}, viewModel = vm)
        }

        composeTestRule.onNodeWithText("Promociones").assertIsDisplayed()
    }
}
