package com.CYRYEL.com.data.cart

import com.CYRYEL.com.TestCoroutineRule
import com.CYRYEL.com.data.product.Product
import com.CYRYEL.com.data.promotion.Promotion
import com.CYRYEL.com.data.promotion.PromotionProduct
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CartManagerTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val cartManager = CartManager()

    private val product = Product(
        id = "prod-1",
        nombre = "Producto Test",
        precio = 25.0,
        stock = 20,
        codigo = "P001"
    )

    private val noStockProduct = Product(
        id = "prod-nostock",
        nombre = "Sin Stock",
        precio = 10.0,
        stock = 2,
        codigo = "P002"
    )

    @Test
    fun `addProduct adds item to empty cart`() = runTest {
        cartManager.addProduct(product)

        val items = cartManager.items.value
        assertEquals(1, items.size)
        assertEquals("prod-1", items[0].productId)
        assertEquals(1, items[0].quantity)
        assertEquals(25.0, items[0].price, 0.001)
        assertEquals(25.0, items[0].subtotal, 0.001)
    }

    @Test
    fun `addProduct increments quantity for existing item`() = runTest {
        cartManager.addProduct(product)
        cartManager.addProduct(product)

        val items = cartManager.items.value
        assertEquals(1, items.size)
        assertEquals(2, items[0].quantity)
        assertEquals(50.0, items[0].subtotal, 0.001)
    }

    @Test
    fun `addProduct with variant creates separate item`() = runTest {
        cartManager.addProduct(product, variantName = "Grande", variantPrice = 30.0)
        cartManager.addProduct(product, variantName = "Pequeño", variantPrice = 20.0)

        val items = cartManager.items.value
        assertEquals(2, items.size)
        assertEquals("Grande", items[0].variantName)
        assertEquals("Pequeño", items[1].variantName)
    }

    @Test
    fun `addProduct with same variant increments quantity`() = runTest {
        cartManager.addProduct(product, variantName = "Grande", variantPrice = 30.0)
        cartManager.addProduct(product, variantName = "Grande", variantPrice = 30.0)

        val items = cartManager.items.value
        assertEquals(1, items.size)
        assertEquals(2, items[0].quantity)
        assertEquals(60.0, items[0].subtotal, 0.001)
    }

    @Test
    fun `addProduct does not add when stock is zero`() = runTest {
        val zeroStockProduct = Product(
            id = "prod-zero",
            nombre = "Zero Stock",
            precio = 10.0,
            stock = 0,
            codigo = "P003"
        )
        cartManager.addProduct(zeroStockProduct)

        val items = cartManager.items.value
        assertEquals(0, items.size)
    }

    @Test
    fun `addProduct respects stock reserve limit`() = runTest {
        val limitedProduct = product.copy(stock = 6)

        repeat(3) { cartManager.addProduct(limitedProduct) }

        val items = cartManager.items.value
        assertEquals(1, items.size)
        assertEquals(2, items[0].quantity)
    }

    @Test
    fun `decreaseProduct reduces quantity`() = runTest {
        cartManager.addProduct(product)
        cartManager.addProduct(product)
        cartManager.decreaseProduct("prod-1")

        val items = cartManager.items.value
        assertEquals(1, items.size)
        assertEquals(1, items[0].quantity)
        assertEquals(25.0, items[0].subtotal, 0.001)
    }

    @Test
    fun `decreaseProduct removes item when quantity reaches zero`() = runTest {
        cartManager.addProduct(product)
        cartManager.decreaseProduct("prod-1")

        assertTrue(cartManager.items.value.isEmpty())
    }

    @Test
    fun `decreaseProduct with variant targets correct item`() = runTest {
        cartManager.addProduct(product, variantName = "Grande", variantPrice = 30.0)
        cartManager.addProduct(product, variantName = "Pequeño", variantPrice = 20.0)
        cartManager.decreaseProduct("prod-1", variantName = "Grande")

        val items = cartManager.items.value
        assertEquals(1, items.size)
        assertEquals("Pequeño", items[0].variantName)
    }

    @Test
    fun `removeProduct removes specific item`() = runTest {
        cartManager.addProduct(product)
        cartManager.addProduct(product.copy(id = "prod-2", nombre = "Producto 2", precio = 15.0))

        cartManager.removeProduct("prod-1")

        val items = cartManager.items.value
        assertEquals(1, items.size)
        assertEquals("prod-2", items[0].productId)
    }

    @Test
    fun `removeProduct with variant removes specific variant`() = runTest {
        cartManager.addProduct(product, variantName = "Grande", variantPrice = 30.0)
        cartManager.addProduct(product, variantName = "Pequeño", variantPrice = 20.0)

        cartManager.removeProduct("prod-1", variantName = "Grande")

        val items = cartManager.items.value
        assertEquals(1, items.size)
        assertEquals("Pequeño", items[0].variantName)
    }

    @Test
    fun `clear empties cart`() = runTest {
        cartManager.addProduct(product)
        cartManager.addProduct(product.copy(id = "prod-2", nombre = "Producto 2"))

        cartManager.clear()

        assertTrue(cartManager.items.value.isEmpty())
    }

    @Test
    fun `getSubtotal returns sum of all subtotals`() = runTest {
        cartManager.addProduct(product)
        cartManager.addProduct(product)
        cartManager.addProduct(product.copy(id = "prod-2", nombre = "Producto 2", precio = 15.0))

        val subtotal = cartManager.getSubtotal()
        assertEquals(65.0, subtotal, 0.001)
    }

    @Test
    fun `addRedeemedProduct adds item with zero price`() = runTest {
        cartManager.addRedeemedProduct(product)

        val items = cartManager.items.value
        assertEquals(1, items.size)
        assertTrue(items[0].redeemedByPoints)
        assertEquals(0.0, items[0].price, 0.001)
        assertEquals(0.0, items[0].subtotal, 0.001)
    }

    @Test
    fun `addRedeemedProduct increments quantity`() = runTest {
        cartManager.addRedeemedProduct(product)
        cartManager.addRedeemedProduct(product)

        val items = cartManager.items.value
        assertEquals(1, items.size)
        assertEquals(2, items[0].quantity)
        assertEquals(0.0, items[0].subtotal, 0.001)
    }

    @Test
    fun `addRedeemedProduct does not exceed stock reserve`() = runTest {
        val limitedProduct = product.copy(stock = 5)

        repeat(3) { cartManager.addRedeemedProduct(limitedProduct) }

        val items = cartManager.items.value
        assertEquals(1, items.size)
        assertEquals(1, items[0].quantity)
    }

    @Test
    fun `addPromotionProducts adds promo items to cart`() = runTest {
        val promotion = Promotion(
            id = "promo-1",
            name = "Combo Ahorro",
            originalPrice = 50.0,
            finalPrice = 40.0,
            products = listOf(
                PromotionProduct(productId = "p1", productName = "Producto 1", quantity = 2, originalPrice = 20.0),
                PromotionProduct(productId = "p2", productName = "Producto 2", quantity = 1, originalPrice = 30.0)
            ),
            stockRemaining = 10
        )

        cartManager.addPromotionProducts(promotion)

        val items = cartManager.items.value
        assertEquals(2, items.size)
        assertEquals("promo-1", items[0].promotionId)
        assertEquals("promo-1", items[1].promotionId)
        assertEquals(2, items[0].quantity)
        assertEquals(1, items[1].quantity)
    }

    @Test
    fun `addPromotionProducts does not add same promotion twice`() = runTest {
        val promotion = Promotion(
            id = "promo-1",
            name = "Combo",
            originalPrice = 50.0,
            finalPrice = 40.0,
            products = listOf(
                PromotionProduct(productId = "p1", productName = "Producto 1", quantity = 1, originalPrice = 50.0)
            ),
            stockRemaining = 10
        )

        cartManager.addPromotionProducts(promotion)
        cartManager.addPromotionProducts(promotion)

        assertEquals(1, cartManager.items.value.size)
    }

    @Test
    fun `addPromotionProducts respects stockRemaining zero`() = runTest {
        val promotion = Promotion(
            id = "promo-1",
            name = "Combo",
            originalPrice = 50.0,
            finalPrice = 40.0,
            products = listOf(
                PromotionProduct(productId = "p1", productName = "Producto 1", quantity = 1, originalPrice = 50.0)
            ),
            stockRemaining = 0
        )

        cartManager.addPromotionProducts(promotion)

        assertTrue(cartManager.items.value.isEmpty())
    }

    @Test
    fun `cart items state flow emits updates`() = runTest {
        cartManager.items.test {
            assertEquals(0, awaitItem().size)
            cartManager.addProduct(product)
            assertEquals(1, awaitItem().size)
            cartManager.addProduct(product.copy(id = "prod-2"))
            assertEquals(2, awaitItem().size)
            cartManager.clear()
            assertEquals(0, awaitItem().size)
        }
    }

    @Test
    fun `mixed regular and redeemed products coexist`() = runTest {
        cartManager.addProduct(product)
        cartManager.addRedeemedProduct(product)

        val items = cartManager.items.value
        assertEquals(2, items.size)
        assertFalse(items[0].redeemedByPoints)
        assertTrue(items[1].redeemedByPoints)
    }

    @Test
    fun `removeProduct with promotionId filters correctly`() = runTest {
        val promo = Promotion(
            id = "promo-1",
            name = "Combo",
            originalPrice = 20.0,
            finalPrice = 15.0,
            products = listOf(
                PromotionProduct(productId = "p1", productName = "P1", quantity = 1, originalPrice = 20.0)
            ),
            stockRemaining = 5
        )

        cartManager.addProduct(product)
        cartManager.addPromotionProducts(promo)

        cartManager.removeProduct("p1", promotionId = "promo-1")

        assertEquals(1, cartManager.items.value.size)
        assertEquals("prod-1", cartManager.items.value[0].productId)
    }
}
