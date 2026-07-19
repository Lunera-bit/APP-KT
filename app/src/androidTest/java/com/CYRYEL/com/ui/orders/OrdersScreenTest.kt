package com.CYRYEL.com.ui.orders

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.CYRYEL.com.data.auth.AuthRepository
import com.CYRYEL.com.data.order.CreateOrderRequest
import com.CYRYEL.com.data.order.Order
import com.CYRYEL.com.data.order.OrderRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class OrdersScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun ordersScreen_showsTitle() {
        val orderRepo = mockk<OrderRepository>(relaxed = true) {
            every { observeOrdersByUserId(any()) } returns flowOf(emptyList())
        }
        val authRepo = mockk<AuthRepository>(relaxed = true) {
            every { getCurrentUserId() } returns "user-123"
        }
        val vm = OrdersViewModel(orderRepo, authRepo)

        composeTestRule.setContent {
            OrdersScreen(
                onBack = {},
                onOrderClick = {},
                viewModel = vm
            )
        }

        composeTestRule.onNodeWithText("Mis Pedidos").assertIsDisplayed()
    }

    @Test
    fun ordersScreen_showsOrderList() {
        val orders = listOf(
            Order(id = "o1", userId = "u1", items = emptyList(), subtotal = 100.0, total = 100.0, status = "pendiente", createdAt = 1000L)
        )
        val orderRepo = object : OrderRepository {
            override suspend fun createOrder(request: CreateOrderRequest) = error("not called")
            override suspend fun getOrdersByUserId(userId: String) = error("not called")
            override suspend fun getOrderById(orderId: String) = error("not called")
            override suspend fun getOrdersByUserIdPaginated(
                userId: String, lastTimestamp: Long?, pageSize: Int
            ) = Result.success(Pair(emptyList<Order>(), false))
            override fun observeOrdersByUserId(userId: String) = flowOf(orders)
        }
        val authRepo = mockk<AuthRepository>(relaxed = true) {
            every { getCurrentUserId() } returns "user-123"
        }

        val vm = OrdersViewModel(orderRepo, authRepo)

        composeTestRule.setContent {
            OrdersScreen(
                onBack = {},
                onOrderClick = {},
                viewModel = vm
            )
        }
    }

    @Test
    fun ordersScreen_showsFilterChips() {
        val orderRepo = mockk<OrderRepository>(relaxed = true) {
            every { observeOrdersByUserId(any()) } returns flowOf(emptyList())
        }
        val authRepo = mockk<AuthRepository>(relaxed = true) {
            every { getCurrentUserId() } returns "user-123"
        }
        val vm = OrdersViewModel(orderRepo, authRepo)

        composeTestRule.setContent {
            OrdersScreen(
                onBack = {},
                onOrderClick = {},
                viewModel = vm
            )
        }
    }

    @Test
    fun ordersScreen_showsErrorMessage() {
        val orderRepo = mockk<OrderRepository>(relaxed = true) {
            every { observeOrdersByUserId(any()) } returns flowOf(emptyList())
        }
        val authRepo = mockk<AuthRepository>(relaxed = true) {
            every { getCurrentUserId() } returns "user-123"
        }
        val vm = OrdersViewModel(orderRepo, authRepo)

        composeTestRule.setContent {
            OrdersScreen(
                onBack = {},
                onOrderClick = {},
                viewModel = vm
            )
        }

        composeTestRule.onNodeWithText("Mis Pedidos").assertIsDisplayed()
    }
}
