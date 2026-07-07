package com.CYRYEL.com.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.CYRYEL.com.data.product.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CatalogUiState(isLoading = true))
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    init {
        loadProducts()
    }

    fun loadProducts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = productRepository.getRandomProducts(limit = 20)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        products = result.getOrDefault(emptyList()),
                        errorMessage = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.localizedMessage ?: "No se pudo cargar catalogo"
                    )
                }
            }
        }
    }
}
