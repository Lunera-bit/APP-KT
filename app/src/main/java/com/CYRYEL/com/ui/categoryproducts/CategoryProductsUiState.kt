package com.CYRYEL.com.ui.categoryproducts

import com.CYRYEL.com.data.product.Product

data class CategoryProductsUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val products: List<Product> = emptyList(),
    val categoryName: String = "",
    val errorMessage: String? = null,
    val lastDocId: String? = null,
    val hasMore: Boolean = true,
    val searchQuery: String = "",
    val searchResults: List<Product>? = null,
    val isSearching: Boolean = false
)
