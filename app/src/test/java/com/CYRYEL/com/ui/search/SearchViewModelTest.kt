package com.CYRYEL.com.ui.search

import app.cash.turbine.test
import com.CYRYEL.com.TestCoroutineRule
import com.CYRYEL.com.data.category.Category
import com.CYRYEL.com.data.category.CategoryRepository
import com.CYRYEL.com.data.product.Product
import com.CYRYEL.com.data.product.ProductRepository
import io.mockk.coEvery
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    @RelaxedMockK
    private lateinit var productRepository: ProductRepository

    @RelaxedMockK
    private lateinit var categoryRepository: CategoryRepository

    private lateinit var viewModel: SearchViewModel

    private val sampleCategories = listOf(
        Category(id = "c1", name = "Lacteos"),
        Category(id = "c2", name = "Bebidas")
    )

    private val sampleProducts = listOf(
        Product(id = "p1", nombre = "Leche Gloria", precio = 5.0, stock = 50, codigo = "P001"),
        Product(id = "p2", nombre = "Yogurt", precio = 8.0, stock = 30, codigo = "P002")
    )

    @Before
    fun setUp() {
        coEvery { categoryRepository.getCategories() } returns Result.success(sampleCategories)
        coEvery { productRepository.searchProducts(any(), any()) } returns Result.success(sampleProducts)
        viewModel = SearchViewModel(productRepository, categoryRepository)
    }

    @Test
    fun `init loads categories`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.categories.size)
            assertFalse(state.hasSearched)
        }
    }

    @Test
    fun `onQueryChange with blank resets to category view`() = runTest {
        viewModel.onQueryChange("test")
        viewModel.onQueryChange("")

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.query.isBlank())
            assertTrue(state.showCategories)
            assertEquals(2, state.categories.size)
        }
    }

    @Test
    fun `onQueryChange searches after debounce`() = runTest {
        viewModel.onQueryChange("leche")

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("leche", state.query)
        }
    }

    @Test
    fun `search returns results`() = runTest {
        viewModel.onQueryChange("leche")

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.results.size)
            assertEquals("Leche Gloria", state.results[0].nombre)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `search handles error`() = runTest {
        coEvery { productRepository.searchProducts(any(), any()) } returns Result.failure(Exception("Sin resultados"))

        viewModel.onQueryChange("xyz")

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.results.isEmpty())
        }
    }
}
