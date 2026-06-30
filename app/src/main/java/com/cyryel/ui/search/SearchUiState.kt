package com.cyryel.ui.search

import com.cyryel.data.product.Product

data class SearchUiState(
    val query: String = "",
    val results: List<Product> = emptyList(),
    val isSearching: Boolean = false,
    val errorMessage: String? = null
)
