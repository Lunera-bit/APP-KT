package com.CYRYEL.com.ui.home

import app.cash.turbine.test
import com.CYRYEL.com.TestCoroutineRule
import com.CYRYEL.com.data.auth.AuthRepository
import com.CYRYEL.com.data.category.Category
import com.CYRYEL.com.data.category.CategoryRepository
import com.CYRYEL.com.data.notificacion.NotificacionData
import com.CYRYEL.com.data.notificacion.NotificacionRepository
import com.CYRYEL.com.data.product.Product
import com.CYRYEL.com.data.product.ProductRepository
import com.CYRYEL.com.data.promotion.Promotion
import com.CYRYEL.com.data.promotion.PromotionRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
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
class HomeViewModelTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    @RelaxedMockK
    private lateinit var productRepository: ProductRepository

    @RelaxedMockK
    private lateinit var categoryRepository: CategoryRepository

    @RelaxedMockK
    private lateinit var promotionRepository: PromotionRepository

    @RelaxedMockK
    private lateinit var notificacionRepository: NotificacionRepository

    @RelaxedMockK
    private lateinit var authRepository: AuthRepository

    private lateinit var viewModel: HomeViewModel

    private val sampleProducts = listOf(
        Product(id = "p1", nombre = "Producto 1", precio = 10.0, stock = 50, codigo = "P001"),
        Product(id = "p2", nombre = "Producto 2", precio = 20.0, stock = 30, codigo = "P002"),
        Product(id = "p3", nombre = "Producto 3", precio = 30.0, stock = 3, codigo = "P003"),
        Product(id = "p4", nombre = "Producto 4", precio = 40.0, stock = 10, codigo = "P004"),
        Product(id = "p5", nombre = "Producto 5", precio = 50.0, stock = 0, codigo = "P005"),
        Product(id = "p6", nombre = "Producto 6", precio = 60.0, stock = 100, codigo = "P006"),
        Product(id = "p7", nombre = "Producto 7", precio = 70.0, stock = 15, codigo = "P007")
    )

    private val sampleCategories = listOf(
        Category(id = "c1", name = "Categoria 1", orden = 1),
        Category(id = "c2", name = "Categoria 2", orden = 2)
    )

    private val samplePromotions = listOf(
        Promotion(id = "promo-1", name = "Promo 1", isActive = true)
    )

    @Before
    fun setUp() {
        every { promotionRepository.getActivePromotions() } returns flowOf(samplePromotions)
        every { authRepository.getCurrentUserId() } returns "user-123"
        every { notificacionRepository.getNotificaciones(any()) } returns flowOf(emptyList())
        coEvery { productRepository.getRandomProducts(limit = 10) } returns Result.success(sampleProducts)
        coEvery { categoryRepository.getCategories() } returns Result.success(sampleCategories)

        viewModel = HomeViewModel(
            productRepository, categoryRepository, promotionRepository,
            notificacionRepository, authRepository
        )
    }

    @Test
    fun `init loads data and shows products`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertNull(state.errorMessage)
            assertEquals(6, state.quickProducts.size)
            assertEquals("Producto 1", state.quickProducts[0].nombre)
        }
    }

    @Test
    fun `init filters out low stock products`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(6, state.quickProducts.size)
            assertTrue(state.quickProducts.none { it.nombre == "Producto 3" })
            assertTrue(state.quickProducts.none { it.nombre == "Producto 5" })
        }
    }

    @Test
    fun `init loads categories`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.categories.size)
        }
    }

    @Test
    fun `init loads promotions`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.promotions.size)
            assertEquals("Promo 1", state.promotions[0].name)
        }
    }

    @Test
    fun `init handles products load failure`() = runTest {
        coEvery { productRepository.getRandomProducts(limit = 10) } returns Result.failure(Exception("Network error"))

        val vm = HomeViewModel(
            productRepository, categoryRepository, promotionRepository,
            notificacionRepository, authRepository
        )

        vm.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals("Network error", state.errorMessage)
            assertTrue(state.quickProducts.isEmpty())
        }
    }

    @Test
    fun `refreshQuickProducts updates products`() = runTest {
        val newProducts = listOf(
            Product(id = "p10", nombre = "Nuevo Producto", precio = 100.0, stock = 20, codigo = "P010")
        )
        coEvery { productRepository.getRandomProducts(limit = 10) } returns Result.success(newProducts)

        viewModel.refreshQuickProducts()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.quickProducts.size)
            assertEquals("Nuevo Producto", state.quickProducts[0].nombre)
        }
    }

    @Test
    fun `refreshQuickProducts respects rate limit`() = runTest {
        val initial = viewModel.uiState.value.quickProducts.toList()
        coEvery { productRepository.getRandomProducts(limit = 10) } returns Result.success(
            listOf(Product(id = "p10", nombre = "Blocked", precio = 100.0, stock = 20, codigo = "P010"))
        )

        viewModel.refreshQuickProducts()
        viewModel.refreshQuickProducts()
        viewModel.refreshQuickProducts()

        assertEquals(initial.map { it.id }, viewModel.uiState.value.quickProducts.map { it.id })
    }

    @Test
    fun `cargarNotifCount loads unread count`() = runTest {
        val notis = listOf(
            NotificacionData(id = "n1", read = false),
            NotificacionData(id = "n2", read = true),
            NotificacionData(id = "n3", read = false)
        )
        every { notificacionRepository.getNotificaciones(any()) } returns flowOf(notis)

        viewModel.cargarNotifCount()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.unreadNotifCount)
        }
    }

    @Test
    fun `cargarNotifCount handles no userId`() {
        every { authRepository.getCurrentUserId() } returns null

        viewModel.cargarNotifCount()

        assertEquals(0, viewModel.uiState.value.unreadNotifCount)
    }

    @Test
    fun `marcarAgregarTodoUsado sets flag`() {
        viewModel.marcarAgregarTodoUsado()
        assertTrue(viewModel.uiState.value.agregarTodoUsado)
    }

    @Test
    fun `promotions are observed via flow`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.promotions.size)
        }
    }
}
