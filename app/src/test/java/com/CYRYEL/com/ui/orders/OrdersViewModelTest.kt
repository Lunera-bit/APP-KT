package com.CYRYEL.com.ui.orders

import app.cash.turbine.test
import com.CYRYEL.com.TestCoroutineRule
import com.CYRYEL.com.data.auth.AuthRepository
import com.CYRYEL.com.data.order.Order
import com.CYRYEL.com.data.order.OrderRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OrdersViewModelTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    @RelaxedMockK
    private lateinit var orderRepository: OrderRepository

    @RelaxedMockK
    private lateinit var authRepository: AuthRepository

    private lateinit var viewModel: OrdersViewModel

    private val sampleOrders = listOf(
        Order(id = "ord-1", userId = "user-123", items = emptyList(), subtotal = 100.0, total = 100.0, status = "pendiente", createdAt = 1000L),
        Order(id = "ord-2", userId = "user-123", items = emptyList(), subtotal = 200.0, total = 200.0, status = "entregado", createdAt = 2000L),
        Order(id = "ord-3", userId = "user-123", items = emptyList(), subtotal = 50.0, total = 50.0, status = "cancelado", createdAt = 3000L)
    )

    @Before
    fun setUp() {
        every { authRepository.getCurrentUserId() } returns "user-123"
        every { orderRepository.observeOrdersByUserId("user-123") } returns flowOf(sampleOrders)
        coEvery { orderRepository.getOrdersByUserIdPaginated(any(), any(), any()) } returns Result.success(Pair(emptyList(), false))

        viewModel = OrdersViewModel(orderRepository, authRepository)
    }

    @Test
    fun `init loads orders`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(3, state.allOrders.size)
            assertEquals(3, state.filteredOrders.size)
        }
    }

    @Test
    fun `init with null userId shows error`() = runTest {
        every { authRepository.getCurrentUserId() } returns null

        val vm = OrdersViewModel(orderRepository, authRepository)

        vm.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals("Usuario no autenticado", state.errorMessage)
        }
    }

    @Test
    fun `setFilter filters orders by status`() = runTest {
        viewModel.setFilter("entregado")

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("entregado", state.selectedFilter)
            assertEquals(1, state.filteredOrders.size)
            assertEquals("ord-2", state.filteredOrders[0].id)
        }
    }

    @Test
    fun `setFilter with todos shows all`() = runTest {
        viewModel.setFilter("todos")

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(3, state.filteredOrders.size)
        }
    }

    @Test
    fun `setDateFilter filters by date range`() = runTest {
        viewModel.setDateFilter(startDate = 1500L, endDate = 2500L)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.filteredOrders.size)
            assertEquals("ord-2", state.filteredOrders[0].id)
        }
    }

    @Test
    fun `clearDateFilter removes date filter`() = runTest {
        viewModel.setDateFilter(startDate = 1500L, endDate = 2500L)
        viewModel.clearDateFilter()

        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.startDate)
            assertNull(state.endDate)
            assertEquals(3, state.filteredOrders.size)
        }
    }

    @Test
    fun `loadMoreOrders loads paginated data`() = runTest {
        val extraOrders = listOf(
            Order(id = "ord-4", userId = "user-123", items = emptyList(), subtotal = 75.0, total = 75.0, status = "pendiente", createdAt = 4000L)
        )
        coEvery { orderRepository.getOrdersByUserIdPaginated(any(), any(), any()) } returns Result.success(Pair(extraOrders, false))

        viewModel.loadMoreOrders()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(4, state.allOrders.size)
            assertFalse(state.isLoadingMore)
        }
    }

    @Test
    fun `applyFilters with unknown filter returns empty`() {
        val result = sampleOrders.applyFilters("inexistente", null, null)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `applyFilters with status filters correctly`() {
        val result = sampleOrders.applyFilters("pendiente", null, null)
        assertEquals(1, result.size)
        assertEquals("ord-1", result[0].id)
    }
}
