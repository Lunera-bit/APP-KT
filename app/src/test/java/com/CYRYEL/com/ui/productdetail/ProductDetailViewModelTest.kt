package com.CYRYEL.com.ui.productdetail

import app.cash.turbine.test
import com.CYRYEL.com.TestCoroutineRule
import com.CYRYEL.com.data.cart.CartManager
import com.CYRYEL.com.data.product.Product
import com.CYRYEL.com.data.product.ProductRepository
import com.CYRYEL.com.data.product.ProductVariant
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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
class ProductDetailViewModelTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    @RelaxedMockK
    private lateinit var productRepository: ProductRepository

    @RelaxedMockK
    private lateinit var cartManager: CartManager

    private lateinit var viewModel: ProductDetailViewModel

    private val sampleProduct = Product(
        id = "prod-1",
        nombre = "Producto Test",
        precio = 25.0,
        stock = 20,
        codigo = "P001",
        variantes = listOf(
            ProductVariant("Grande", 30.0, 1),
            ProductVariant("Pequeño", 20.0, 1)
        ),
        categoria = "Lacteos"
    )

    @Before
    fun setUp() {
        every { cartManager.items } returns MutableStateFlow(emptyList())
        coEvery { productRepository.observeProduct(any()) } returns flowOf(Result.success(sampleProduct))

        viewModel = ProductDetailViewModel(productRepository, cartManager)
    }

    @Test
    fun `loadProduct loads product successfully`() = runTest {
        viewModel.loadProduct("prod-1")

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertNotNull(state.product)
            assertEquals("Producto Test", state.product?.nombre)
            assertEquals(25.0, state.displayPrice, 0.001)
            assertNull(state.errorMessage)
        }
    }

    @Test
    fun `loadProduct handles error`() = runTest {
        coEvery { productRepository.observeProduct(any()) } returns flowOf(Result.failure(Exception("Producto no encontrado")))

        viewModel.loadProduct("prod-404")

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertNull(state.product)
            assertEquals("Producto no encontrado", state.errorMessage)
        }
    }

    @Test
    fun `selectVariant updates price and quantity`() = runTest {
        viewModel.loadProduct("prod-1")

        viewModel.selectVariant(0)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(0, state.selectedVariantIndex)
            assertEquals(30.0, state.displayPrice, 0.001)
        }
    }

    @Test
    fun `increaseQuantity increments by 1 normally`() = runTest {
        viewModel.loadProduct("prod-1")

        viewModel.increaseQuantity()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.quantity)
        }
    }

    @Test
    fun `decreaseQuantity decrements by 1 normally`() = runTest {
        viewModel.loadProduct("prod-1")

        viewModel.increaseQuantity()
        viewModel.increaseQuantity()
        viewModel.decreaseQuantity()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.quantity)
        }
    }

    @Test
    fun `decreaseQuantity does not go below 1`() = runTest {
        viewModel.loadProduct("prod-1")

        viewModel.decreaseQuantity()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.quantity)
        }
    }

    @Test
    fun `addToCart adds product to cart`() = runTest {
        viewModel.loadProduct("prod-1")

        val result = viewModel.addToCart()

        assertTrue(result)
    }

    @Test
    fun `addToCart returns false when no product loaded`() {
        val result = viewModel.addToCart()
        assertFalse(result)
    }

    @Test
    fun `addToCart with variant adds to cart with variant`() = runTest {
        viewModel.loadProduct("prod-1")
        viewModel.selectVariant(1)

        val result = viewModel.addToCart()

        assertTrue(result)
    }

    @Test
    fun `displayStock uses availableStock minus cartQuantity`() = runTest {
        every { cartManager.items } returns MutableStateFlow(
            listOf(com.CYRYEL.com.data.cart.CartItem(
                productId = "prod-1", productName = "Producto Test",
                quantity = 2, price = 25.0, subtotal = 50.0, product = sampleProduct
            ))
        )

        viewModel.loadProduct("prod-1")

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.cartQuantity)
        }
    }

    @Test
    fun `selectVariant with -1 resets to default price`() = runTest {
        viewModel.loadProduct("prod-1")
        viewModel.selectVariant(0)
        viewModel.selectVariant(-1)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(-1, state.selectedVariantIndex)
            assertEquals(25.0, state.displayPrice, 0.001)
        }
    }

    @Test
    fun `addToCart adds quantity times`() = runTest {
        viewModel.loadProduct("prod-1")
        viewModel.increaseQuantity()
        viewModel.increaseQuantity()

        viewModel.addToCart()
    }
}
