package com.CYRYEL.com.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.CYRYEL.com.data.product.ProductVariant
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: ProductDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.productDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndReadProducts() = runBlocking {
        val products = listOf(
            ProductEntity(
                id = "p1", nombre = "Producto 1", categoria = "Cat1", codigo = "C001",
                foto = "", precio = 10.0, stock = 100, unidad = "unidad",
                description = "", isActive = true, keywords = emptyList(),
                variantes = emptyList(), points = 0, pointsToRedeem = 0,
                updatedAt = 1000L, cachedAt = 1000L
            ),
            ProductEntity(
                id = "p2", nombre = "Producto 2", categoria = "Cat2", codigo = "C002",
                foto = "", precio = 20.0, stock = 50, unidad = "unidad",
                description = "", isActive = true, keywords = emptyList(),
                variantes = emptyList(), points = 0, pointsToRedeem = 0,
                updatedAt = 2000L, cachedAt = 2000L
            )
        )

        dao.upsertAll(products)
        val loaded = dao.getAllProducts()

        assertEquals(2, loaded.size)
        assertEquals("Producto 1", loaded[0].nombre)
    }

    @Test
    fun upsertReplacesExistingProduct() = runBlocking {
        val original = ProductEntity(
            id = "p1", nombre = "Original", categoria = "Cat1", codigo = "C001",
            foto = "", precio = 10.0, stock = 100, unidad = "unidad",
            description = "", isActive = true, keywords = emptyList(),
            variantes = emptyList(), points = 0, pointsToRedeem = 0,
            updatedAt = 1000L, cachedAt = 1000L
        )
        dao.upsertAll(listOf(original))

        val updated = original.copy(nombre = "Actualizado", precio = 15.0)
        dao.upsertAll(listOf(updated))

        val loaded = dao.getAllProducts()
        assertEquals(1, loaded.size)
        assertEquals("Actualizado", loaded[0].nombre)
        assertEquals(15.0, loaded[0].precio, 0.001)
    }

    @Test
    fun deleteAllExceptRemovesUnlistedProducts() = runBlocking {
        val products = listOf(
            ProductEntity(
                id = "p1", nombre = "Keep", categoria = "Cat1", codigo = "C001",
                foto = "", precio = 10.0, stock = 100, unidad = "unidad",
                description = "", isActive = true, keywords = emptyList(),
                variantes = emptyList(), points = 0, pointsToRedeem = 0,
                updatedAt = 1000L, cachedAt = 1000L
            ),
            ProductEntity(
                id = "p2", nombre = "Remove", categoria = "Cat2", codigo = "C002",
                foto = "", precio = 20.0, stock = 50, unidad = "unidad",
                description = "", isActive = true, keywords = emptyList(),
                variantes = emptyList(), points = 0, pointsToRedeem = 0,
                updatedAt = 2000L, cachedAt = 2000L
            ),
            ProductEntity(
                id = "p3", nombre = "Keep Too", categoria = "Cat1", codigo = "C003",
                foto = "", precio = 30.0, stock = 30, unidad = "unidad",
                description = "", isActive = true, keywords = emptyList(),
                variantes = emptyList(), points = 0, pointsToRedeem = 0,
                updatedAt = 3000L, cachedAt = 3000L
            )
        )
        dao.upsertAll(products)

        dao.deleteAllExcept(listOf("p1", "p3"))

        val loaded = dao.getAllProducts()
        assertEquals(2, loaded.size)
        assertTrue(loaded.all { it.id != "p2" })
    }

    @Test
    fun getAllProductsReturnsEmptyForEmptyDatabase() = runBlocking {
        val products = dao.getAllProducts()
        assertTrue(products.isEmpty())
    }

    @Test
    fun upsertAllWithVariantsSavesCorrectly() = runBlocking {
        val variants = listOf(
            ProductVariant("Grande", 30.0, 1),
            ProductVariant("Pequeño", 20.0, 1)
        )
        val product = ProductEntity(
            id = "p1", nombre = "Con Variantes", categoria = "Cat1", codigo = "C001",
            foto = "", precio = 25.0, stock = 50, unidad = "unidad",
            description = "", isActive = true, keywords = listOf("a", "b"),
            variantes = variants, points = 10, pointsToRedeem = 100,
            updatedAt = 1000L, cachedAt = 1000L
        )

        dao.upsertAll(listOf(product))

        val loaded = dao.getAllProducts()
        assertEquals(1, loaded.size)
        assertEquals(2, loaded[0].variantes.size)
        assertEquals("Grande", loaded[0].variantes[0].nombre)
    }
}
