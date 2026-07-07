package com.cyryel.ui.search

import com.cyryel.data.category.Category
import com.cyryel.data.product.Product

data class SearchUiState(
    val query: String = "",
    val results: List<Product> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val hasSearched: Boolean get() = query.isNotBlank()
    val showCategories: Boolean get() = query.isBlank()
}
