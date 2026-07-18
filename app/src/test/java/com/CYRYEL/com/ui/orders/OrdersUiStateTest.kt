package com.CYRYEL.com.ui.orders

import com.CYRYEL.com.data.order.Order
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OrdersUiStateTest {

    private val sampleOrders = listOf(
        Order(id = "o1", userId = "u1", items = emptyList(), subtotal = 100.0, total = 100.0, status = "pendiente", createdAt = 1000L),
        Order(id = "o2", userId = "u1", items = emptyList(), subtotal = 200.0, total = 200.0, status = "entregado", createdAt = 2000L),
        Order(id = "o3", userId = "u1", items = emptyList(), subtotal = 50.0, total = 50.0, status = "pendiente", createdAt = 3000L),
        Order(id = "o4", userId = "u1", items = emptyList(), subtotal = 75.0, total = 75.0, status = "cancelado", createdAt = 1500L)
    )

    @Test
    fun `applyFilters with todos returns all orders`() {
        val result = sampleOrders.applyFilters("todos", null, null)
        assertEquals(4, result.size)
    }

    @Test
    fun `applyFilters filters by status`() {
        val result = sampleOrders.applyFilters("pendiente", null, null)
        assertEquals(2, result.size)
        assertTrue(result.all { it.status == "pendiente" })
    }

    @Test
    fun `applyFilters with entregado returns only delivered`() {
        val result = sampleOrders.applyFilters("entregado", null, null)
        assertEquals(1, result.size)
        assertEquals("o2", result[0].id)
    }

    @Test
    fun `applyFilters with cancelado returns only cancelled`() {
        val result = sampleOrders.applyFilters("cancelado", null, null)
        assertEquals(1, result.size)
        assertEquals("o4", result[0].id)
    }

    @Test
    fun `applyFilters with startDate filters correctly`() {
        val result = sampleOrders.applyFilters("todos", startDate = 2000L, endDate = null)
        assertEquals(2, result.size)
        assertTrue(result.all { it.createdAt >= 2000L })
    }

    @Test
    fun `applyFilters with endDate filters correctly`() {
        val result = sampleOrders.applyFilters("todos", startDate = null, endDate = 2000L)
        assertEquals(2, result.size)
        assertTrue(result.all { it.createdAt <= 2000L })
    }

    @Test
    fun `applyFilters with date range filters correctly`() {
        val result = sampleOrders.applyFilters("todos", startDate = 1000L, endDate = 2000L)
        assertEquals(2, result.size)
    }

    @Test
    fun `applyFilters combines status and date filter`() {
        val result = sampleOrders.applyFilters("pendiente", startDate = 1500L, endDate = null)
        assertEquals(1, result.size)
        assertEquals("o3", result[0].id)
    }

    @Test
    fun `applyFilters with unknown status returns empty`() {
        val result = sampleOrders.applyFilters("inexistente", null, null)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `applyFilters with null dates returns all`() {
        val result = sampleOrders.applyFilters("todos", null, null)
        assertEquals(4, result.size)
    }

    @Test
    fun `OrdersUiState default values are correct`() {
        val state = OrdersUiState()
        assertTrue(state.isLoading)
        assertTrue(state.allOrders.isEmpty())
        assertEquals("todos", state.selectedFilter)
        assertTrue(state.hasMore)
        assertNull(state.startDate)
        assertNull(state.endDate)
    }

    @Test
    fun `OrdersUiState orders property returns allOrders`() {
        val state = OrdersUiState(allOrders = sampleOrders)
        assertEquals(4, state.orders.size)
    }
}
