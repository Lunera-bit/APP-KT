package com.cyryel.ui.categoryproducts

import com.cyryel.data.product.Product

data class CategoryProductsUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val products: List<Product> = emptyList(),
    val categoryName: String = "",
    val errorMessage: String? = null,
    val lastDocId: String? = null,
    val hasMore: Boolean = true
)
