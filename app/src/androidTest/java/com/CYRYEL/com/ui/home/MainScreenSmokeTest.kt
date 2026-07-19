package com.CYRYEL.com.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.CYRYEL.com.data.auth.AuthRepository
import com.CYRYEL.com.data.cart.CartManager
import com.CYRYEL.com.data.category.Category
import com.CYRYEL.com.data.category.CategoryRepository
import com.CYRYEL.com.data.notificacion.NotificacionRepository
import com.CYRYEL.com.data.product.Product
import com.CYRYEL.com.data.product.ProductRepository
import com.CYRYEL.com.data.promotion.PromotionRepository
import com.CYRYEL.com.ui.cart.CartViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class MainScreenSmokeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun grantNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .executeShellCommand("pm grant com.CYRYEL.com android.permission.POST_NOTIFICATIONS")
                .close()
        }
    }

    @Test
    fun mainScreen_showsHomeTab() {
        val productRepo = object : ProductRepository {
            override suspend fun getRandomProducts(limit: Int) = Result.success(
                listOf(Product(id = "p1", nombre = "Producto Test", precio = 10.0, stock = 50, codigo = "P001"))
            )
            override suspend fun getProductById(productId: String) = error("not called")
            override fun observeProduct(productId: String) = error("not called")
            override suspend fun getProductsByCategory(category: String, limit: Int) =
                Result.success(emptyList<Product>())
            override suspend fun getProductsByCategoryPaged(category: String, limit: Int, startAfter: String?) =
                error("not called")
            override suspend fun searchProducts(query: String, limit: Int) = Result.success(emptyList<Product>())
            override suspend fun searchProductsByCategory(query: String, category: String, limit: Int) =
                Result.success(emptyList<Product>())
            override suspend fun getCategories() = error("not called")
            override suspend fun getRedeemableProducts(limit: Int) = Result.success(emptyList<Product>())
        }
        val categoryRepo = object : CategoryRepository {
            override suspend fun getCategories() = Result.success(emptyList<Category>())
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
        val cartManager = CartManager()

        val homeViewModel = HomeViewModel(productRepo, categoryRepo, promotionRepo, notifRepo, authRepo)
        val cartViewModel = CartViewModel(cartManager)

        composeTestRule.setContent {
            MainScreen(
                onNavigateToNotifications = {},
                onNavigateToSearch = {},
                onNavigateToOffers = {},
                onNavigateToCart = {},
                onNavigateToSettings = {},
                onNavigateToProduct = {},
                homeViewModel = homeViewModel,
                cartViewModel = cartViewModel
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Producto Test").assertIsDisplayed()
    }
}
