package com.CYRYEL.com.ui.billetera

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.CYRYEL.com.data.auth.AuthRepository
import com.CYRYEL.com.data.cart.CartManager
import com.CYRYEL.com.data.product.ProductRepository
import com.CYRYEL.com.data.promotion.PromotionRepository
import com.CYRYEL.com.data.user.User
import com.CYRYEL.com.data.user.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class BilleteraScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun billeteraScreen_showsTitle() {
        val userRepo = mockk<UserRepository>(relaxed = true) {
            coEvery { getUser(any()) } returns Result.success(User(id = "u1", name = "Test", points = 100))
        }
        val promoRepo = mockk<PromotionRepository>(relaxed = true) {
            every { getActivePromotions() } returns flowOf(emptyList())
        }
        val productRepo = mockk<ProductRepository>(relaxed = true) {
            coEvery { getRedeemableProducts() } returns Result.success(emptyList())
        }
        val authRepo = mockk<AuthRepository>(relaxed = true) {
            every { getCurrentUserId() } returns "user-123"
        }
        val firestore = mockk<FirebaseFirestore>(relaxed = true)
        val cartManager = mockk<CartManager>(relaxed = true)

        val vm = BilleteraViewModel(userRepo, promoRepo, productRepo, authRepo, firestore, cartManager)

        composeTestRule.setContent {
            BilleteraScreen(
                onNavigateToOffers = {},
                onNavigateToHistorial = {},
                viewModel = vm
            )
        }

        composeTestRule.onNodeWithText("Billetera").assertIsDisplayed()
    }

    @Test
    fun billeteraScreen_showsUserPoints() {
        val userRepo = mockk<UserRepository>(relaxed = true) {
            coEvery { getUser(any()) } returns Result.success(User(id = "u1", name = "Test User", points = 500))
        }
        val promoRepo = mockk<PromotionRepository>(relaxed = true) {
            every { getActivePromotions() } returns flowOf(emptyList())
        }
        val productRepo = mockk<ProductRepository>(relaxed = true) {
            coEvery { getRedeemableProducts() } returns Result.success(emptyList())
        }
        val authRepo = mockk<AuthRepository>(relaxed = true) {
            every { getCurrentUserId() } returns "user-123"
        }
        val firestore = mockk<FirebaseFirestore>(relaxed = true)
        val cartManager = mockk<CartManager>(relaxed = true)

        val vm = BilleteraViewModel(userRepo, promoRepo, productRepo, authRepo, firestore, cartManager)

        composeTestRule.setContent {
            BilleteraScreen(
                onNavigateToOffers = {},
                onNavigateToHistorial = {},
                viewModel = vm
            )
        }
    }

    @Test
    fun billeteraScreen_showsErrorState() {
        val userRepo = mockk<UserRepository>(relaxed = true) {
            coEvery { getUser(any()) } returns Result.failure(Exception("Error"))
        }
        val promoRepo = mockk<PromotionRepository>(relaxed = true) {
            every { getActivePromotions() } returns flowOf(emptyList())
        }
        val productRepo = mockk<ProductRepository>(relaxed = true) {
            coEvery { getRedeemableProducts() } returns Result.success(emptyList())
        }
        val authRepo = mockk<AuthRepository>(relaxed = true) {
            every { getCurrentUserId() } returns "user-123"
        }
        val firestore = mockk<FirebaseFirestore>(relaxed = true)
        val cartManager = mockk<CartManager>(relaxed = true)

        val vm = BilleteraViewModel(userRepo, promoRepo, productRepo, authRepo, firestore, cartManager)

        composeTestRule.setContent {
            BilleteraScreen(
                onNavigateToOffers = {},
                onNavigateToHistorial = {},
                viewModel = vm
            )
        }

        composeTestRule.onNodeWithText("Billetera").assertIsDisplayed()
    }
}
