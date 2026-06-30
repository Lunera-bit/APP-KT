package com.cyryel.ui.catalog

import com.cyryel.data.product.Product

data class CatalogUiState(
    val isLoading: Boolean = false,
    val products: List<Product> = emptyList(),
    val errorMessage: String? = null
)
