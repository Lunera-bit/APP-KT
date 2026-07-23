package com.CYRYEL.com.ui.catalog

import app.cash.turbine.test
import com.CYRYEL.com.TestCoroutineRule
import com.CYRYEL.com.data.product.Product
import com.CYRYEL.com.data.product.ProductRepository
import io.mockk.coEvery
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModelTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    @RelaxedMockK
    private lateinit var productRepository: ProductRepository

    private lateinit var viewModel: CatalogViewModel

    private val sampleProducts = listOf(
        Product(id = "p1", nombre = "Producto 1", precio = 10.0, stock = 50, codigo = "P001"),
        Product(id = "p2", nombre = "Producto 2", precio = 20.0, stock = 30, codigo = "P002"),
        Product(id = "p3", nombre = "Producto 3", precio = 30.0, stock = 20, codigo = "P003")
    )

    @Before
    fun setUp() {
        coEvery { productRepository.getRandomProducts(limit = 20) } returns Result.success(sampleProducts)
        viewModel = CatalogViewModel(productRepository)
    }

    @Test
    fun `init loads products`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(3, state.products.size)
            assertNull(state.errorMessage)
        }
    }

    @Test
    fun `loadProducts with failure shows error`() = runTest {
        coEvery { productRepository.getRandomProducts(limit = 20) } returns Result.failure(Exception("Error de red"))

        viewModel.loadProducts()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals("Error de red", state.errorMessage)
        }
    }

    @Test
    fun `loadProducts maintains loading state`() = runTest {
        viewModel.uiState.test {
            val initial = awaitItem()
            assertFalse(initial.isLoading)
        }
    }
}
