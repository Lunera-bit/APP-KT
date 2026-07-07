package com.CYRYEL.com.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.CYRYEL.com.data.cart.CartManager
import com.CYRYEL.com.data.product.Product
import com.CYRYEL.com.data.promotion.Promotion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartManager: CartManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CartUiState())
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            cartManager.items.collect { items ->
                _uiState.update { it.copy(items = items) }
            }
        }
    }

    fun addProduct(product: Product, variantName: String? = null, variantPrice: Double? = null) {
        cartManager.addProduct(product, variantName, variantPrice)
    }

    fun decreaseProduct(productId: String, variantName: String? = null, redeemedByPoints: Boolean = false) {
        cartManager.decreaseProduct(productId, variantName, redeemedByPoints)
    }

    fun removeProduct(productId: String, variantName: String? = null, redeemedByPoints: Boolean = false, promotionId: String? = null) {
        cartManager.removeProduct(productId, variantName, redeemedByPoints, promotionId)
    }

    fun addPromotionToCart(promotion: Promotion) {
        cartManager.addPromotionProducts(promotion)
    }

    fun clearCart() {
        cartManager.clear()
    }
}
