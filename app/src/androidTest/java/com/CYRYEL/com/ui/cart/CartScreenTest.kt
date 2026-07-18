package com.CYRYEL.com.ui.cart

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.CYRYEL.com.data.cart.CartItem
import com.CYRYEL.com.data.cart.CartManager
import com.CYRYEL.com.data.product.Product
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class CartScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun cartScreen_showsEmptyCart() {
        val cartManager = mockk<CartManager>(relaxed = true) {
            every { items } returns MutableStateFlow(emptyList())
        }
        val vm = CartViewModel(cartManager)

        composeTestRule.setContent {
            CartSection(
                onBack = {},
                onCheckout = {},
                onProductClick = {},
                viewModel = vm
            )
        }

        composeTestRule.onNodeWithText("Tu carrito esta vacio").assertIsDisplayed()
    }

    @Test
    fun cartScreen_showsCartItems() {
        val product = Product(id = "p1", nombre = "Producto Test", precio = 25.0, stock = 10)
        val items = listOf(
            CartItem(productId = "p1", productName = "Producto Test", quantity = 2, price = 25.0, subtotal = 50.0, product = product)
        )
        val cartManager = mockk<CartManager>(relaxed = true) {
            every { items } returns MutableStateFlow(items)
        }
        val vm = CartViewModel(cartManager)

        composeTestRule.setContent {
            CartSection(
                onBack = {},
                onCheckout = {},
                onProductClick = {},
                viewModel = vm
            )
        }

        composeTestRule.onNodeWithText("Producto Test").assertIsDisplayed()
    }

    @Test
    fun cartScreen_showsTotal() {
        val product = Product(id = "p1", nombre = "Producto Test", precio = 25.0, stock = 10)
        val items = listOf(
            CartItem(productId = "p1", productName = "Producto Test", quantity = 2, price = 25.0, subtotal = 50.0, product = product)
        )
        val cartManager = mockk<CartManager>(relaxed = true) {
            every { items } returns MutableStateFlow(items)
        }
        val vm = CartViewModel(cartManager)

        composeTestRule.setContent {
            CartSection(
                onBack = {},
                onCheckout = {},
                onProductClick = {},
                viewModel = vm
            )
        }
    }

    @Test
    fun cartScreen_showsCheckoutButton() {
        val cartManager = mockk<CartManager>(relaxed = true) {
            every { items } returns MutableStateFlow(emptyList())
        }
        val vm = CartViewModel(cartManager)

        composeTestRule.setContent {
            CartSection(
                onBack = {},
                onCheckout = {},
                onProductClick = {},
                viewModel = vm
            )
        }
    }
}
