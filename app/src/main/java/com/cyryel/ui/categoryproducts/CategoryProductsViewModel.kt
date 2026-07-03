package com.cyryel.ui.categoryproducts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyryel.data.product.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    private var searchJob: Job? = null

    fun loadCategory(categoryName: String) {
        if (_uiState.value.categoryName == categoryName && _uiState.value.products.isNotEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, categoryName = categoryName, errorMessage = null) }
            val result = productRepository.getProductsByCategoryPaged(categoryName)
            if (result.isSuccess) {
                val page = result.getOrNull()!!
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        products = page.products,
                        lastDocId = page.lastDocId,
                        hasMore = page.lastDocId != null
                    )
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

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore || state.searchQuery.isNotBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            val result = productRepository.getProductsByCategoryPaged(
                category = state.categoryName,
                startAfter = state.lastDocId
            )
            if (result.isSuccess) {
                val page = result.getOrNull()!!
                _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        products = it.products + page.products,
                        lastDocId = page.lastDocId,
                        hasMore = page.lastDocId != null
                    )
                }
            } else {
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = null, isSearching = false) }
            searchJob?.cancel()
            return
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            delay(300)
            val result = productRepository.searchProductsByCategory(
                query = query,
                category = _uiState.value.categoryName
            )
            result.onSuccess { products ->
                _uiState.update { it.copy(searchResults = products, isSearching = false) }
            }
            result.onFailure {
                _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            }
        }
    }
}
