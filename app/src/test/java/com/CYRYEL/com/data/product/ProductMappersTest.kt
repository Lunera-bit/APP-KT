package com.CYRYEL.com.data.product

import com.CYRYEL.com.data.local.ProductEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ProductMappersTest {

    private val sampleProduct = Product(
        id = "p1",
        nombre = "Producto Test",
        categoria = "Lacteos",
        codigo = "P001",
        foto = "https://example.com/img.jpg",
        precio = 25.50,
        stock = 100,
        unidad = "unidad",
        description = "Descripcion del producto",
        isActive = true,
        keywords = listOf("leche", "lacteo"),
        variantes = listOf(
            ProductVariant("Grande", 30.0, 1)
        ),
        points = 10,
        pointsToRedeem = 100,
        updatedAt = 1000L
    )

    @Test
    fun `Product toEntity maps all fields correctly`() {
        val cachedAt = 2000L
        val entity = sampleProduct.toEntity(cachedAt)

        assertEquals("p1", entity.id)
        assertEquals("Producto Test", entity.nombre)
        assertEquals("Lacteos", entity.categoria)
        assertEquals("P001", entity.codigo)
        assertEquals("https://example.com/img.jpg", entity.foto)
        assertEquals(25.50, entity.precio, 0.001)
        assertEquals(100, entity.stock)
        assertEquals("unidad", entity.unidad)
        assertEquals("Descripcion del producto", entity.description)
        assertEquals(true, entity.isActive)
        assertEquals(listOf("leche", "lacteo"), entity.keywords)
        assertEquals(1, entity.variantes.size)
        assertEquals("Grande", entity.variantes[0].nombre)
        assertEquals(10, entity.points)
        assertEquals(100, entity.pointsToRedeem)
        assertEquals(1000L, entity.updatedAt)
        assertEquals(cachedAt, entity.cachedAt)
    }

    @Test
    fun `ProductEntity toDomain maps all fields correctly`() {
        val entity = ProductEntity(
            id = "p1",
            nombre = "Producto Test",
            categoria = "Lacteos",
            codigo = "P001",
            foto = "https://example.com/img.jpg",
            precio = 25.50,
            stock = 100,
            unidad = "unidad",
            description = "Descripcion del producto",
            isActive = true,
            keywords = listOf("leche", "lacteo"),
            variantes = listOf(ProductVariant("Grande", 30.0, 1)),
            points = 10,
            pointsToRedeem = 100,
            updatedAt = 1000L,
            cachedAt = 2000L
        )

        val product = entity.toDomain()

        assertEquals("p1", product.id)
        assertEquals("Producto Test", product.nombre)
        assertEquals("Lacteos", product.categoria)
        assertEquals("P001", product.codigo)
        assertEquals("https://example.com/img.jpg", product.foto)
        assertEquals(25.50, product.precio, 0.001)
        assertEquals(100, product.stock)
        assertEquals("unidad", product.unidad)
        assertEquals("Descripcion del producto", product.description)
        assertEquals(true, product.isActive)
        assertEquals(listOf("leche", "lacteo"), product.keywords)
        assertEquals(1, product.variantes.size)
        assertEquals(10, product.points)
        assertEquals(100, product.pointsToRedeem)
        assertEquals(1000L, product.updatedAt)
    }

    @Test
    fun `toEntity and toDomain are inverse operations`() {
        val cachedAt = 3000L
        val entity = sampleProduct.toEntity(cachedAt)
        val backToProduct = entity.toDomain()

        assertEquals(sampleProduct.id, backToProduct.id)
        assertEquals(sampleProduct.nombre, backToProduct.nombre)
        assertEquals(sampleProduct.precio, backToProduct.precio, 0.001)
        assertEquals(sampleProduct.stock, backToProduct.stock)
        assertEquals(sampleProduct.variantes.size, backToProduct.variantes.size)
    }

    @Test
    fun `toEntity with empty fields maps correctly`() {
        val emptyProduct = Product()
        val entity = emptyProduct.toEntity(0L)

        assertEquals("", entity.id)
        assertEquals("", entity.nombre)
        assertEquals(0, entity.stock)
        assertEquals(0.0, entity.precio, 0.001)
        assertEquals(true, entity.isActive)
    }

    @Test
    fun `toDomain with empty entity maps correctly`() {
        val entity = ProductEntity(
            id = "", nombre = "", categoria = "", codigo = "", foto = "",
            precio = 0.0, stock = 0, unidad = "", description = "",
            isActive = true, keywords = emptyList(), variantes = emptyList(),
            points = 0, pointsToRedeem = 0, updatedAt = 0L, cachedAt = 0L
        )

        val product = entity.toDomain()
        assertEquals("", product.id)
        assertEquals("", product.nombre)
        assertEquals(0, product.stock)
    }

    @Test
    fun `toEntity preserves cachedAt timestamp`() {
        val cachedAt = System.currentTimeMillis()
        val entity = sampleProduct.toEntity(cachedAt)
        assertEquals(cachedAt, entity.cachedAt)
    }
}
