package com.cyryel.ui.search

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
class SearchViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { SearchUiState() }
            return
        }
        _uiState.update { it.copy(query = query, isLoading = true, errorMessage = null) }
        searchJob = viewModelScope.launch {
            delay(300)
            val result = productRepository.searchProducts(query)
            _uiState.update {
                it.copy(
                    results = result.getOrDefault(emptyList()),
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.localizedMessage
                )
            }
        }
    }
}
