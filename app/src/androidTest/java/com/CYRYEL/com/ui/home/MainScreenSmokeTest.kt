package com.CYRYEL.com.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.CYRYEL.com.data.auth.AuthRepository
import com.CYRYEL.com.data.cart.CartManager
import com.CYRYEL.com.data.category.CategoryRepository
import com.CYRYEL.com.data.notificacion.NotificacionRepository
import com.CYRYEL.com.data.product.Product
import com.CYRYEL.com.data.product.ProductRepository
import com.CYRYEL.com.data.promotion.PromotionRepository
import com.CYRYEL.com.ui.cart.CartViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class MainScreenSmokeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mainScreen_showsHomeTab() {
        val productRepo = mockk<ProductRepository>(relaxed = true) {
            coEvery { getRandomProducts(limit = 10) } returns Result.success(
                listOf(Product(id = "p1", nombre = "Producto Test", precio = 10.0, stock = 50, codigo = "P001"))
            )
        }
        val categoryRepo = mockk<CategoryRepository>(relaxed = true) {
            coEvery { getCategories() } returns Result.success(emptyList())
        }
        val promotionRepo = mockk<PromotionRepository>(relaxed = true) {
            every { getActivePromotions() } returns flowOf(emptyList())
        }
        val notifRepo = mockk<NotificacionRepository>(relaxed = true) {
            every { getNotificaciones(any()) } returns flowOf(emptyList())
        }
        val authRepo = mockk<AuthRepository>(relaxed = true) {
            every { getCurrentUserId() } returns "user-123"
        }
        val cartManager = mockk<CartManager>(relaxed = true) {
            every { items } returns kotlinx.coroutines.flow.MutableStateFlow(emptyList())
        }

        val homeViewModel = HomeViewModel(productRepo, categoryRepo, promotionRepo, notifRepo, authRepo)
        val cartViewModel = CartViewModel(cartManager)

        composeTestRule.setContent {
            MainScreen(
                homeViewModel = homeViewModel,
                cartViewModel = cartViewModel
            )
        }

        composeTestRule.onNodeWithText("Producto Test").assertIsDisplayed()
    }
}
