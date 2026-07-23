package com.CYRYEL.com.data.product

import org.junit.Assert.assertEquals
import org.junit.Test

class AvailableStockTest {

    @Test
    fun `availableStock returns stock minus reserve for high stock`() {
        val product = Product(id = "p1", stock = 100)
        assertEquals(96, product.availableStock)
    }

    @Test
    fun `availableStock returns stock minus reserve for low stock`() {
        val product = Product(id = "p1", stock = 6)
        assertEquals(2, product.availableStock)
    }

    @Test
    fun `availableStock returns 0 when stock equals reserve`() {
        val product = Product(id = "p1", stock = 4)
        assertEquals(0, product.availableStock)
    }

    @Test
    fun `availableStock returns 0 when stock below reserve`() {
        val product = Product(id = "p1", stock = 2)
        assertEquals(0, product.availableStock)
    }

    @Test
    fun `availableStock returns 0 for zero stock`() {
        val product = Product(id = "p1", stock = 0)
        assertEquals(0, product.availableStock)
    }

    @Test
    fun `availableStock handles negative stock gracefully`() {
        val product = Product(id = "p1", stock = -5)
        assertEquals(0, product.availableStock)
    }

    @Test
    fun `STOCK_RESERVE constant is 4`() {
        assertEquals(4, STOCK_RESERVE)
    }
}
