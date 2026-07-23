package com.CYRYEL.com.data

import com.CYRYEL.com.data.product.Product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ForcedPackConfigTest {

    private val knownProductId = "041e6520-991a-439e-b888-ad609642fb58"
    private val knownCodigo = "20250725172121"

    private val knownProduct = Product(
        id = knownProductId,
        nombre = "Pack Huevos",
        codigo = knownCodigo,
        stock = 24
    )

    private val unknownProduct = Product(
        id = "unknown-id",
        nombre = "Producto Normal",
        codigo = "NORMAL001"
    )

    @Test
    fun `getPackSize returns correct size for known product by id`() {
        val size = ForcedPackConfig.getPackSize(knownProduct)
        assertNotNull(size)
        assertEquals(4, size)
    }

    @Test
    fun `getPackSize returns correct size for known product by codigo`() {
        val product = Product(
            id = "new-id",
            codigo = knownCodigo,
            nombre = "Producto"
        )
        val size = ForcedPackConfig.getPackSize(product)
        assertNotNull(size)
        assertEquals(4, size)
    }

    @Test
    fun `getPackSize returns null for unknown product`() {
        val size = ForcedPackConfig.getPackSize(unknownProduct)
        assertNull(size)
    }

    @Test
    fun `getPackSize with id only returns correct size`() {
        val size = ForcedPackConfig.getPackSize(productId = knownProductId)
        assertEquals(4, size)
    }

    @Test
    fun `getPackSize with id and codigo prefers id match`() {
        val size = ForcedPackConfig.getPackSize(productId = knownProductId, codigo = "nonexistent")
        assertEquals(4, size)
    }

    @Test
    fun `getPackSize with codigo only works`() {
        val size = ForcedPackConfig.getPackSize(productId = "unknown", codigo = knownCodigo)
        assertEquals(4, size)
    }

    @Test
    fun `isForcedPackProduct returns true for known product`() {
        assertTrue(ForcedPackConfig.isForcedPackProduct(knownProduct))
    }

    @Test
    fun `isForcedPackProduct returns false for unknown product`() {
        assertFalse(ForcedPackConfig.isForcedPackProduct(unknownProduct))
    }

    @Test
    fun `isForcedPackProduct by id returns true`() {
        assertTrue(ForcedPackConfig.isForcedPackProduct(productId = knownProductId))
    }

    @Test
    fun `isForcedPackProduct by id returns false for unknown`() {
        assertFalse(ForcedPackConfig.isForcedPackProduct(productId = "unknown"))
    }

    @Test
    fun `allForcedPackIds contains known ids`() {
        val ids = ForcedPackConfig.allForcedPackIds
        assertTrue(ids.contains(knownProductId))
        assertTrue(ids.contains(knownCodigo))
    }

    @Test
    fun `allForcedPackIds does not contain unknown ids`() {
        val ids = ForcedPackConfig.allForcedPackIds
        assertFalse(ids.contains("unknown-id"))
    }

    @Test
    fun `multiple known products return different pack sizes`() {
        val size1 = ForcedPackConfig.getPackSize(productId = "041e6520-991a-439e-b888-ad609642fb58")
        val size6 = ForcedPackConfig.getPackSize(productId = "35fa9ad7-d37d-4391-9368-0f9e394b7ab7")
        val size12 = ForcedPackConfig.getPackSize(productId = "8f3011c9-a95d-4f14-b510-73c621420457")

        assertEquals(4, size1)
        assertEquals(6, size6)
        assertEquals(12, size12)
    }
}
