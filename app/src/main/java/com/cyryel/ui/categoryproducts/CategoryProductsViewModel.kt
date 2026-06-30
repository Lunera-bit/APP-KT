package com.cyryel.ui.categoryproducts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyryel.data.product.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryProductsViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryProductsUiState())
    val uiState: StateFlow<CategoryProductsUiState> = _uiState.asStateFlow()

    fun loadCategory(categoryName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, categoryName = categoryName, errorMessage = null) }
            val result = productRepository.getProductsByCategory(categoryName)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(isLoading = false, products = result.getOrDefault(emptyList()))
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Error al cargar productos"
                    )
                }
            }
        }
    }
}
