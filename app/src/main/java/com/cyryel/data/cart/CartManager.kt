package com.cyryel.data.cart

import com.cyryel.data.local.CartDao
import com.cyryel.data.local.CartItemEntity
import com.cyryel.data.product.Product
import com.cyryel.data.product.ProductVariant
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CartManager @Inject constructor(
    private val cartDao: CartDao
) {

    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _items = MutableStateFlow<List<CartItem>>(emptyList())
    val items: StateFlow<List<CartItem>> = _items.asStateFlow()

    init {
        scope.launch {
            val entities = cartDao.getAll()
            _items.value = entities.map { it.toDomain() }
        }
    }

    fun addProduct(product: Product, variantName: String? = null, variantPrice: Double? = null) {
        if (product.stock <= 0) return
        val price = variantPrice ?: product.precio
        _items.update { current ->
            val existing = current.find { it.productId == product.id && it.variantName == variantName }
            val currentQty = (existing?.quantity ?: 0) + 1
            if (currentQty > product.stock) return@update current
            val updated = if (existing == null) {
                current + CartItem(
                    productId = product.id,
                    productName = product.nombre,
                    quantity = 1,
                    price = price,
                    subtotal = price,
                    product = product,
                    variantName = variantName
                )
            } else {
                current.map { item ->
                    if (item.productId == product.id && item.variantName == variantName) {
                        val qty = item.quantity + 1
                        item.copy(
                            quantity = qty,
                            subtotal = item.price * qty
                        )
                    } else {
                        item
                    }
                }
            }
            persist(updated)
            updated
        }
    }

    fun decreaseProduct(productId: String, variantName: String? = null) {
        _items.update { current ->
            val updated = current.mapNotNull { item ->
                if (item.productId != productId || item.variantName != variantName) {
                    item
                } else {
                    val qty = item.quantity - 1
                    if (qty <= 0) {
                        null
                    } else {
                        item.copy(quantity = qty, subtotal = item.price * qty)
                    }
                }
            }
            persist(updated)
            updated
        }
    }

    fun removeProduct(productId: String, variantName: String? = null) {
        _items.update { current ->
            val updated = current.filterNot { it.productId == productId && it.variantName == variantName }
            persist(updated)
            updated
        }
    }

    fun clear() {
        _items.value = emptyList()
        scope.launch { cartDao.deleteAll() }
    }

    fun getSubtotal(): Double {
        return _items.value.sumOf { it.subtotal }
    }

    private fun persist(items: List<CartItem>) {
        scope.launch {
            cartDao.deleteAll()
            if (items.isNotEmpty()) {
                cartDao.upsertAll(items.map { it.toEntity() })
            }
        }
    }

    private fun CartItem.toEntity() = CartItemEntity(
        compositeId = productId + (variantName?.let { "_$it" } ?: ""),
        productId = productId,
        productName = productName,
        quantity = quantity,
        price = price,
        subtotal = subtotal,
        productJson = gson.toJson(product),
        variantName = variantName
    )

    private fun CartItemEntity.toDomain(): CartItem {
        val product: Product = try {
            val type = object : TypeToken<Product>() {}.type
            gson.fromJson(productJson, type)
        } catch (_: Exception) {
            Product(id = productId, nombre = productName)
        }
        return CartItem(
            productId = productId,
            productName = productName,
            quantity = quantity,
            price = price,
            subtotal = subtotal,
            product = product,
            variantName = variantName
        )
    }

}
