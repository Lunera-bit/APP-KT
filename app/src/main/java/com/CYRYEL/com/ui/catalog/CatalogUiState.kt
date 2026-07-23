package com.CYRYEL.com.ui.catalog

import com.CYRYEL.com.data.product.Product

data class CatalogUiState(
    val isLoading: Boolean = false,
    val products: List<Product> = emptyList(),
    val errorMessage: String? = null
)
