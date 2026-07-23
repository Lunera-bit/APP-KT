package com.CYRYEL.com.ui.productdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.CYRYEL.com.data.ForcedPackConfig
import com.CYRYEL.com.data.cart.CartManager
import com.CYRYEL.com.data.product.ProductRepository
import com.CYRYEL.com.data.product.availableStock
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
        _uiState.update { it.copy(isLoading = true, errorMessage = null, product = null) }
        viewModelScope.launch {
            productRepository.observeProduct(productId).collect { result ->
                if (result.isSuccess) {
                    val product = result.getOrNull()
                    val forcedPackSize = product?.let(ForcedPackConfig::getPackSize)
                    val cartQty = cartManager.items.value.filter { it.productId == productId }.sumOf { it.quantity }
                    val initialQty = if (forcedPackSize != null) {
                        val remaining = (product?.availableStock ?: 0) - cartQty
                        val raw = maxOf(forcedPackSize, 1)
                        if (raw <= remaining) raw else forcedPackSize
                    } else 1
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            product = product,
                            quantity = initialQty,
                            selectedVariantIndex = -1,
                            forcedPackSize = forcedPackSize,
                            cartQuantity = cartQty
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
    }

    fun selectVariant(index: Int) {
        val quantity = if (index >= 0) {
            _uiState.value.product?.variantes?.get(index)?.cantidad?.toInt() ?: 1
        } else {
            1
        }
        _uiState.update { it.copy(selectedVariantIndex = index, quantity = quantity) }
    }

    fun increaseQuantity() {
        val state = _uiState.value
        val maxStock = state.displayStock
        val pack = state.forcedPackSize
        if (pack != null) {
            val next = state.quantity + pack
            if (next <= maxStock) {
                _uiState.update { it.copy(quantity = next) }
            }
        } else if (state.quantity < maxStock) {
            _uiState.update { it.copy(quantity = it.quantity + 1) }
        }
    }

    fun decreaseQuantity() {
        val state = _uiState.value
        val pack = state.forcedPackSize
        if (pack != null) {
            val next = state.quantity - pack
            if (next >= pack) {
                _uiState.update { it.copy(quantity = next) }
            }
        } else if (state.quantity > 1) {
            _uiState.update { it.copy(quantity = it.quantity - 1) }
        }
    }

    fun addToCart(): Boolean {
        val state = _uiState.value
        val product = state.product ?: return false

        val pack = state.forcedPackSize
        if (pack != null) {
            if (state.quantity < pack || state.quantity % pack != 0) return false
        }

        val variantName: String? = if (state.selectedVariantIndex >= 0) {
            product.variantes[state.selectedVariantIndex].nombre
        } else null
        val variantPrice: Double? = if (state.selectedVariantIndex >= 0) {
            product.variantes[state.selectedVariantIndex].precio
        } else null
        repeat(state.quantity) {
            cartManager.addProduct(product, variantName, variantPrice)
        }
        return true
    }


}
