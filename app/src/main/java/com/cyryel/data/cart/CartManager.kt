package com.cyryel.data.cart

import com.cyryel.data.product.Product
import com.cyryel.data.product.ProductVariant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CartManager @Inject constructor() {

    private val _items = MutableStateFlow<List<CartItem>>(emptyList())
    val items: StateFlow<List<CartItem>> = _items.asStateFlow()

    fun addProduct(product: Product, variantName: String? = null, variantPrice: Double? = null) {
        if (product.stock <= 0) return
        val price = variantPrice ?: product.precio
        _items.update { current ->
            val existing = current.find { it.productId == product.id && it.variantName == variantName }
            val currentQty = (existing?.quantity ?: 0) + 1
            if (currentQty > product.stock) return@update current
            if (existing == null) {
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
                        item.copy(quantity = qty, subtotal = item.price * qty)
                    } else {
                        item
                    }
                }
            }
        }
    }

    fun decreaseProduct(productId: String, variantName: String? = null) {
        _items.update { current ->
            current.mapNotNull { item ->
                if (item.productId != productId || item.variantName != variantName) {
                    item
                } else {
                    val qty = item.quantity - 1
                    if (qty <= 0) null
                    else item.copy(quantity = qty, subtotal = item.price * qty)
                }
            }
        }
    }

    fun removeProduct(productId: String, variantName: String? = null) {
        _items.update { current ->
            current.filterNot { it.productId == productId && it.variantName == variantName }
        }
    }

    fun clear() {
        _items.value = emptyList()
    }

    fun getSubtotal(): Double {
        return _items.value.sumOf { it.subtotal }
    }

}
