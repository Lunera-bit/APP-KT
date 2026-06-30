package com.cyryel.ui.productdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyryel.data.cart.CartManager
import com.cyryel.data.product.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val cartManager: CartManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    fun loadProduct(productId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = productRepository.getProductById(productId)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        product = result.getOrNull(),
                        quantity = 1,
                        selectedVariantIndex = -1
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Error al cargar producto"
                    )
                }
            }
        }
    }

    fun selectVariant(index: Int) {
        _uiState.update { it.copy(selectedVariantIndex = index, quantity = 1) }
    }

    fun increaseQuantity() {
        val state = _uiState.value
        val maxStock = state.displayStock
        if (state.quantity < maxStock) {
            _uiState.update { it.copy(quantity = it.quantity + 1) }
        }
    }

    fun decreaseQuantity() {
        val state = _uiState.value
        if (state.quantity > 1) {
            _uiState.update { it.copy(quantity = it.quantity - 1) }
        }
    }

    fun addToCart(): Boolean {
        val state = _uiState.value
        val product = state.product ?: return false
        repeat(state.quantity) {
            cartManager.addProduct(product)
        }
        return true
    }
}
