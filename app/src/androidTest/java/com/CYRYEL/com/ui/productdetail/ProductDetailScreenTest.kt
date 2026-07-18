package com.CYRYEL.com.ui.productdetail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.CYRYEL.com.data.cart.CartManager
import com.CYRYEL.com.data.product.Product
import com.CYRYEL.com.data.product.ProductRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ProductDetailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun productDetailScreen_showsLoading() {
        val productRepo = mockk<ProductRepository>(relaxed = true) {
            coEvery { observeProduct(any()) } returns flowOf(Result.success(null))
        }
        val cartManager = mockk<CartManager>(relaxed = true) {
            every { items } returns MutableStateFlow(emptyList())
        }
        val vm = ProductDetailViewModel(productRepo, cartManager)

        composeTestRule.setContent {
            ProductDetailScreen(
                productId = "p1",
                onBack = {},
                onNavigateToCart = {},
                viewModel = vm
            )
        }
    }

    @Test
    fun productDetailScreen_showsProductName() {
        val product = Product(id = "p1", nombre = "Producto Test", precio = 25.0, stock = 10, codigo = "P001")
        val productRepo = mockk<ProductRepository>(relaxed = true) {
            coEvery { observeProduct(any()) } returns flowOf(Result.success(product))
        }
        val cartManager = mockk<CartManager>(relaxed = true) {
            every { items } returns MutableStateFlow(emptyList())
        }
        val vm = ProductDetailViewModel(productRepo, cartManager)

        composeTestRule.setContent {
            ProductDetailScreen(
                productId = "p1",
                onBack = {},
                onNavigateToCart = {},
                viewModel = vm
            )
        }

        composeTestRule.onNodeWithText("Producto Test").assertIsDisplayed()
    }
}
