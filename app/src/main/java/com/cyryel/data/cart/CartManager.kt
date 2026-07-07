package com.cyryel.data.cart

import com.cyryel.data.product.Product
import com.cyryel.data.product.ProductVariant
import com.cyryel.data.product.availableStock
import com.cyryel.data.promotion.Promotion
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

    private fun totalProductQuantity(items: List<CartItem>, productId: String): Int {
        return items.filter { it.productId == productId }.sumOf { it.quantity }
    }

    fun addProduct(product: Product, variantName: String? = null, variantPrice: Double? = null) {
        if (product.availableStock <= 0) return
        val price = variantPrice ?: product.precio
        _items.update { current ->
            if (totalProductQuantity(current, product.id) + 1 > product.availableStock) return@update current
            val existing = current.find { it.productId == product.id && it.variantName == variantName && !it.redeemedByPoints }
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
                    if (item.productId == product.id && item.variantName == variantName && !item.redeemedByPoints) {
                        val qty = item.quantity + 1
                        item.copy(quantity = qty, subtotal = item.price * qty)
                    } else {
                        item
                    }
                }
            }
        }
    }

    fun decreaseProduct(productId: String, variantName: String? = null, redeemedByPoints: Boolean = false) {
        _items.update { current ->
            current.mapNotNull { item ->
                if (item.productId != productId || item.variantName != variantName || item.redeemedByPoints != redeemedByPoints) {
                    item
                } else {
                    val qty = item.quantity - 1
                    if (qty <= 0) null
                    else item.copy(quantity = qty, subtotal = item.price * qty)
                }
            }
        }
    }

    fun removeProduct(productId: String, variantName: String? = null, redeemedByPoints: Boolean = false) {
        _items.update { current ->
            current.filterNot { it.productId == productId && it.variantName == variantName && it.redeemedByPoints == redeemedByPoints }
        }
    }

    fun addRedeemedProduct(product: Product) {
        if (product.availableStock <= 0) return
        _items.update { current ->
            if (totalProductQuantity(current, product.id) + 1 > product.availableStock) return@update current
            val existing = current.find { it.productId == product.id && it.redeemedByPoints }
            if (existing == null) {
                current + CartItem(
                    productId = product.id,
                    productName = product.nombre,
                    quantity = 1,
                    price = 0.0,
                    subtotal = 0.0,
                    product = product,
                    redeemedByPoints = true
                )
            } else {
                current.map { item ->
                    if (item.productId == product.id && item.redeemedByPoints) {
                        val qty = item.quantity + 1
                        item.copy(quantity = qty, subtotal = 0.0)
                    } else {
                        item
                    }
                }
            }
        }
    }

    fun addPromotionProducts(promotion: Promotion) {
        if (_items.value.any { it.promotionId == promotion.id }) return
        if (promotion.stockRemaining <= 0) return
        val ratio = if (promotion.originalPrice > 0) promotion.finalPrice / promotion.originalPrice else 1.0
        val promoItems = promotion.products.map { promoProduct ->
            val unitPrice = kotlin.math.round(promoProduct.originalPrice * ratio * 100) / 100
            CartItem(
                productId = promoProduct.productId,
                productName = promoProduct.productName,
                quantity = promoProduct.quantity,
                price = unitPrice,
                subtotal = kotlin.math.round(unitPrice * promoProduct.quantity * 100) / 100,
                product = Product(
                    id = promoProduct.productId,
                    nombre = promoProduct.productName,
                    precio = unitPrice,
                    stock = Int.MAX_VALUE,
                    foto = promotion.imageUrl
                ),
                promotionId = promotion.id
            )
        }
        val computedSum = promoItems.sumOf { it.subtotal }
        val diff = kotlin.math.round((promotion.finalPrice - computedSum) * 100) / 100
        if (kotlin.math.abs(diff) > 0.001) {
            val adjusted = promoItems.toMutableList()
            val lastIndex = adjusted.size - 1
            adjusted[lastIndex] = adjusted[lastIndex].copy(
                subtotal = adjusted[lastIndex].subtotal + diff
            )
            _items.update { current -> current + adjusted }
        } else {
            _items.update { current -> current + promoItems }
        }
    }

    fun clear() {
        _items.value = emptyList()
    }

    fun getSubtotal(): Double {
        return _items.value.sumOf { it.subtotal }
    }

}
