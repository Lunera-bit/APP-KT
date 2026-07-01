package com.cyryel.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyryel.data.cart.CartManager
import com.cyryel.data.product.Product
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

    fun addProduct(product: Product) {
        cartManager.addProduct(product)
    }

    fun decreaseProduct(productId: String) {
        cartManager.decreaseProduct(productId)
    }

    fun removeProduct(productId: String) {
        cartManager.removeProduct(productId)
    }

    fun clearCart() {
        cartManager.clear()
    }
}
