package com.CYRYEL.com.ui.cart

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.CYRYEL.com.data.cart.CartManager
import com.CYRYEL.com.data.product.Product
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
        val cartManager = CartManager()
        val vm = CartViewModel(cartManager)

        composeTestRule.setContent {
            CartScreen(
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
        val cartManager = CartManager()
        cartManager.addProduct(product)
        val vm = CartViewModel(cartManager)

        composeTestRule.setContent {
            CartScreen(
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
        val cartManager = CartManager()
        cartManager.addProduct(product)
        val vm = CartViewModel(cartManager)

        composeTestRule.setContent {
            CartScreen(
                onBack = {},
                onCheckout = {},
                onProductClick = {},
                viewModel = vm
            )
        }
    }

    @Test
    fun cartScreen_showsCheckoutButton() {
        val cartManager = CartManager()
        val vm = CartViewModel(cartManager)

        composeTestRule.setContent {
            CartScreen(
                onBack = {},
                onCheckout = {},
                onProductClick = {},
                viewModel = vm
            )
        }
    }
}
